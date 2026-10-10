import test from 'node:test';
import assert from 'node:assert/strict';
import {paymentHandlers,validateRequest,remainingCart,verifiedCapture,requestId,cents,usd} from '../src/payment.mjs';
import {firestoreStore} from '../src/firestore-store.mjs';
import {paypalClient} from '../src/paypal.mjs';

class FakeDb {
 docs=new Map(); failCommit=false; queue=Promise.resolve();
 collection(path) {return {doc:id=>this.ref(`${path}/${id}`)};}
 ref(path) {return {path,id:path.split('/').at(-1),get:async()=>this.snapshot(path)};}
 snapshot(path) {const data=this.docs.get(path);return {id:path.split('/').at(-1),exists:data!==undefined,data:()=>structuredClone(data)};}
 async runTransaction(block) {
  const previous=this.queue;let unlock;this.queue=new Promise(resolve=>unlock=resolve);await previous;
  try {
   const writes=[];
   const result=await block({get:async ref=>this.snapshot(ref.path),create:(ref,value)=>{assert(!this.docs.has(ref.path));writes.push([ref.path,structuredClone(value)]);},set:(ref,value)=>writes.push([ref.path,structuredClone(value)]),update:(ref,value)=>{assert(this.docs.has(ref.path));writes.push([ref.path,{...structuredClone(this.docs.get(ref.path)),...structuredClone(value)}]);}});
   if(this.failCommit) {this.failCommit=false;throw new Error('Atomic commit failed');}
   for(const [path,value] of writes) this.docs.set(path,value);return result;
  } finally {unlock();}
 }
}
const cart=[{id:'line1',productId:'product1',quantity:2,color:null}];
const shipping={fullName:'Test Customer',addressLine1:'12 Main Street',city:'Boston',postalCode:'01234',countryCode:'US',state:'MA'};
function receipt(overrides={}) {return {id:'PAYPAL1',status:'COMPLETED',purchase_units:[{payee:{merchant_id:'MERCHANT'},payments:{captures:[{id:'CAPTURE1',status:'COMPLETED',amount:{value:'20.00',currency_code:'USD'}}]}}],...overrides};}
function setup() {
 const db=new FakeDb();db.docs.set('customer/user1',{id:'user1',cart:structuredClone(cart)});db.docs.set('product/product1',{id:'product1',price:10});
 const calls={create:[],capture:[]};let captureReceipt=receipt();
 const paypal={create:async(p,id)=>{calls.create.push({p:structuredClone(p),id});return {id:'PAYPAL1',links:[{rel:'payer-action',href:'https://www.sandbox.paypal.com/checkoutnow?token=PAYPAL1'}]};},capture:async(id,key)=>{calls.capture.push({id,key});return captureReceipt;}};
 const handlers=paymentHandlers({store:firestoreStore(db),paypal,merchantId:'MERCHANT'});
 const data={checkoutId:'checkout1',expectedAmount:'20.00',cart:structuredClone(cart),shipping:structuredClone(shipping)};
 return {db,calls,handlers,data,setReceipt:value=>captureReceipt=value};
}
test('valid address preserves leading zero and state',()=>validateRequest({checkoutId:'x',expectedAmount:'20.00',cart,shipping}));
for(const [name,change] of [
 ['missing checkout reference',d=>delete d.checkoutId],['empty cart',d=>d.cart=[]],['zero quantity',d=>d.cart[0].quantity=0],['fractional quantity',d=>d.cart[0].quantity=1.5],['excess quantity',d=>d.cart[0].quantity=11],['duplicate line',d=>d.cart.push(d.cart[0])],['raw double amount',d=>d.expectedAmount='0.30000000000000004'],['country as state',d=>d.shipping.state='Usa'],['missing US state',d=>d.shipping.state=''],['trimmed ZIP',d=>d.shipping.postalCode='1234'],['invalid country',d=>d.shipping.countryCode='XX'],['invalid product path',d=>d.cart[0].productId='../other'],
]) test(`rejects ${name}`,()=>{const {data}=setup();change(data);assert.throws(()=>validateRequest(data));});
test('unauthenticated create and complete cannot reach PayPal',async()=>{const {handlers,data,calls}=setup();await assert.rejects(handlers.create(null,data));await assert.rejects(handlers.complete(null,{orderId:'PAYPAL1'}));assert.equal(calls.create.length+calls.capture.length,0);});
test('unknown forged callback cannot create order',async()=>{const {handlers,db,calls}=setup();await assert.rejects(handlers.complete('user1',{orderId:'FORGED'}));assert.equal(calls.capture.length,0);assert(!db.docs.has('order/paypal_FORGED'));});
test('other customer cannot capture pending payment',async()=>{const {handlers,data,calls}=setup();await handlers.create('user1',data);await assert.rejects(handlers.complete('user2',{orderId:'PAYPAL1'}));assert.equal(calls.capture.length,0);});
test('server rejects changed prices and missing products',async()=>{for(const missing of [false,true]) {const {handlers,data,db,calls}=setup();if(missing)db.docs.delete('product/product1');else db.docs.get('product/product1').price=15;await assert.rejects(handlers.create('user1',data));assert.equal(calls.create.length,0);}});
test('server rejects checkout using stale cart',async()=>{const {handlers,data,db}=setup();db.docs.get('customer/user1').cart[0].quantity=1;await assert.rejects(handlers.create('user1',data));});
test('same checkout reuses PayPal order and frozen shipping',async()=>{const {handlers,data,calls}=setup();const first=await handlers.create('user1',data);const second=await handlers.create('user1',data);assert.deepEqual(first,second);assert.equal(calls.create.length,1);assert.equal(calls.create[0].p.shipping.postalCode,'01234');data.shipping.addressLine1='Other address';await assert.rejects(handlers.create('user1',data));});
test('capture stores frozen items/amount despite later cart and price changes',async()=>{const {handlers,data,db}=setup();await handlers.create('user1',data);db.docs.get('product/product1').price=99;db.docs.get('customer/user1').cart.push({id:'new-line',productId:'new-product',quantity:1});const order=await handlers.complete('user1',{orderId:'PAYPAL1'});assert.deepEqual(order.items,cart);assert.equal(order.totalAmount,20);assert.deepEqual(order.shipping,shipping);assert.equal(db.docs.get('customer/user1').cart[0].id,'new-line');});
test('additional quantity survives capture',async()=>{const {handlers,data,db}=setup();await handlers.create('user1',data);db.docs.get('customer/user1').cart[0].quantity=5;await handlers.complete('user1',{orderId:'PAYPAL1'});assert.equal(db.docs.get('customer/user1').cart[0].quantity,3);});
test('callback replay returns one order and captures once',async()=>{const {handlers,data,db,calls}=setup();await handlers.create('user1',data);const a=await handlers.complete('user1',{orderId:'PAYPAL1'});const b=await handlers.complete('user1',{orderId:'PAYPAL1'});assert.equal(a.id,b.id);assert.equal([...db.docs.keys()].filter(k=>k.startsWith('order/')).length,1);assert.equal(calls.capture.length,1);});
test('concurrent confirmations commit one order',async()=>{const {handlers,data,db,calls}=setup();await handlers.create('user1',data);await Promise.all([handlers.complete('user1',{orderId:'PAYPAL1'}),handlers.complete('user1',{orderId:'PAYPAL1'})]);assert.equal([...db.docs.keys()].filter(k=>k.startsWith('order/')).length,1);assert(calls.capture.every(c=>c.key===calls.capture[0].key));});
test('failed atomic commit leaves order absent/cart intact and retry succeeds',async()=>{const {handlers,data,db}=setup();await handlers.create('user1',data);db.failCommit=true;await assert.rejects(handlers.complete('user1',{orderId:'PAYPAL1'}));assert(!db.docs.has('order/paypal_PAYPAL1'));assert.deepEqual(db.docs.get('customer/user1').cart,cart);await handlers.complete('user1',{orderId:'PAYPAL1'});assert(db.docs.has('order/paypal_PAYPAL1'));assert.deepEqual(db.docs.get('customer/user1').cart,[]);});
for(const [name,change] of [
 ['approval only',r=>r.status='APPROVED'],['wrong order',r=>r.id='OTHER'],['pending capture',r=>r.purchase_units[0].payments.captures[0].status='PENDING'],['wrong amount',r=>r.purchase_units[0].payments.captures[0].amount.value='1.00'],['wrong currency',r=>r.purchase_units[0].payments.captures[0].amount.currency_code='EUR'],['wrong merchant',r=>r.purchase_units[0].payee.merchant_id='OTHER'],['multiple captures',r=>r.purchase_units[0].payments.captures.push(r.purchase_units[0].payments.captures[0])],
]) test(`rejects ${name} without creating order or clearing cart`,async()=>{const {handlers,data,db,setReceipt}=setup();await handlers.create('user1',data);const r=receipt();change(r);setReceipt(r);await assert.rejects(handlers.complete('user1',{orderId:'PAYPAL1'}));assert(!db.docs.has('order/paypal_PAYPAL1'));assert.deepEqual(db.docs.get('customer/user1').cart,cart);});
test('money decimal precision and half cents agree with client',()=>{assert.equal(usd(cents(0.1)+cents(0.2)),'0.30');assert.equal(cents(0.005),1);for(const value of [NaN,Infinity,-1])assert.throws(()=>cents(value));});
test('cart reconciliation never removes unrelated products sharing changed id',()=>assert.deepEqual(remainingCart([{id:'line1',productId:'other',quantity:2}],cart),[{id:'line1',productId:'other',quantity:2}]));
test('idempotency keys are stable and differ for capture/create/user',()=>{assert.equal(requestId('u','c','create'),requestId('u','c','create'));assert.notEqual(requestId('u','c','create'),requestId('u','c','capture'));assert.notEqual(requestId('u','c','create'),requestId('v','c','create'));});
test('PayPal HTTP retries already-captured by reading/verifying completed receipt',async()=>{
 const calls=[];const fetcher=async(url,options)=>{calls.push({url,options});if(url.endsWith('/token'))return {ok:true,json:async()=>({access_token:'server-token'})};if(url.endsWith('/capture'))return {ok:false,json:async()=>({details:[{issue:'ORDER_ALREADY_CAPTURED'}]})};return {ok:true,json:async()=>receipt()};};
 const client=paypalClient({clientId:'test',secret:'test',environment:'sandbox',fetcher});
 const result=await client.capture('PAYPAL1','same-key');assert.equal(result.status,'COMPLETED');assert.equal(calls.at(-1).options.method,'GET');assert(calls.filter(c=>!c.url.endsWith('/token')).every(c=>c.options.headers['PayPal-Request-Id']==='same-key'));
});
test('missing capture reference cannot create paid order',()=>{const r=receipt();delete r.purchase_units[0].payments.captures[0].id;assert.throws(()=>verifiedCapture(r,{paypalId:'PAYPAL1',amount:'20.00'},'MERCHANT'));});
test('minimal successful capture is followed by full order lookup before verification',async()=>{
 const calls=[];
 const fetcher=async(url,options)=>{calls.push({url,options});return {ok:true,json:async()=>url.endsWith('/token')?{access_token:'server-token'}:url.endsWith('/capture')?{id:'PAYPAL1',status:'COMPLETED'}:receipt()};};
 const client=paypalClient({clientId:'test',secret:'test',environment:'sandbox',fetcher});
 const r=await client.capture('PAYPAL1','stable-key');assert.equal(verifiedCapture(r,{paypalId:'PAYPAL1',amount:'20.00'},'MERCHANT'),'CAPTURE1');
 assert.equal(calls.at(-1).options.method,'GET');assert(calls.filter(c=>!c.url.endsWith('/token')).every(c=>c.options.headers.Prefer==='return=representation'));
});
