import { createHash } from 'node:crypto';
export class PaymentError extends Error { constructor(code,message) { super(message); this.code=code; } }
export function ensure(ok,message,code='failed-precondition') { if(!ok) throw new PaymentError(code,message); }
export const cents=value=>{ ensure(typeof value==='number' && Number.isFinite(value) && value>0 && value<=1_000_000,'Invalid price.'); return Math.round(value*100); };
export const usd=amount=>`${Math.floor(amount/100)}.${String(amount%100).padStart(2,'0')}`;
export const requestId=(uid,id,phase)=>createHash('sha256').update(`${uid}:${id}:${phase}`).digest('hex');
export function validateRequest(data) {
  ensure(data && /^[A-Za-z0-9_-]{1,64}$/.test(data.checkoutId??''),'Invalid checkout reference.','invalid-argument');
  ensure(Array.isArray(data.cart) && data.cart.length>0 && data.cart.length<=100,'Invalid cart.','invalid-argument');
  const ids=new Set();
  for(const item of data.cart) {
    ensure(item && typeof item.id==='string' && item.id.length>0 && item.id.length<=64 && /^[A-Za-z0-9_-]+$/.test(item.productId??'') && Number.isSafeInteger(item.quantity) && item.quantity>=1 && item.quantity<=10 && !ids.has(item.id),'Invalid cart item.','invalid-argument');
    ids.add(item.id);
  }
  ensure(/^\d+\.\d{2}$/.test(data.expectedAmount??''),'Invalid amount.','invalid-argument');
  const s=data.shipping;
  ensure(s && ['IL','US','IN','RS'].includes(s.countryCode),'Invalid shipping country.','invalid-argument');
  for(const field of ['fullName','addressLine1','city','postalCode']) ensure(typeof s[field]==='string' && s[field].trim().length>0 && s[field].length<=200,'Invalid shipping address.','invalid-argument');
  const zip={IL:/^([0-9]{5}|[0-9]{7})$/,US:/^\d{5}(-\d{4})?$/,IN:/^\d{6}$/,RS:/^\d{5}$/};
  ensure(zip[s.countryCode].test(s.postalCode),'Invalid postal code.','invalid-argument');
  if(s.countryCode==='US') ensure('AL AK AZ AR CA CO CT DE DC FL GA HI ID IL IN IA KS KY LA ME MD MA MI MN MS MO MT NE NV NH NJ NM NY NC ND OH OK OR PA RI SC SD TN TX UT VT VA WA WV WI WY PR GU VI AS MP'.split(' ').includes(s.state),'Use a US state code.','invalid-argument');
  if(s.countryCode==='IN') ensure(typeof s.state==='string' && s.state.trim().length>0 && s.state.length<=100,'State is required.','invalid-argument');
}
export function remainingCart(current,purchased) {
  return current.flatMap(line=>{
    const paid=purchased.find(item=>item.id===line.id && item.productId===line.productId);
    if(!paid) return [line];
    const quantity=line.quantity-paid.quantity;
    return quantity>0 ? [{...line,quantity}] : [];
  });
}
export function verifiedCapture(receipt,pending,merchantId) {
  const units=receipt?.purchase_units, captures=units?.[0]?.payments?.captures;
  ensure(receipt?.id===pending.paypalId && receipt.status==='COMPLETED' && units?.length===1 && captures?.length===1,'Payment is not completed.');
  const c=captures[0];
  ensure(c.status==='COMPLETED' && c.amount?.currency_code==='USD' && c.amount.value===pending.amount && units[0].payee?.merchant_id===merchantId,'Payment amount or merchant does not match.');
  ensure(typeof c.id==='string' && c.id.length>0,'Missing capture reference.');
  return c.id;
}
export function paymentHandlers({store,paypal,merchantId}) {
 return {
  async create(uid,data) {
    ensure(uid,'Please sign in.','unauthenticated'); validateRequest(data);
    const p=await store.prepare(uid,data);
    if(p.paypalId) return {orderId:p.paypalId,approvalUrl:p.approvalUrl,amount:p.amount};
    const response=await paypal.create(p,requestId(uid,data.checkoutId,'create'));
    const approvalUrl=response.links?.find(l=>l.rel==='payer-action'||l.rel==='approve')?.href;
    ensure(response.id && approvalUrl && /^https:\/\/www\.(sandbox\.)?paypal\.com\//.test(approvalUrl),'PayPal approval is unavailable.');
    await store.attach(uid,data.checkoutId,response.id,approvalUrl);
    return {orderId:response.id,approvalUrl,amount:p.amount};
  },
  async complete(uid,data) {
    ensure(uid,'Please sign in.','unauthenticated');
    ensure(/^[A-Za-z0-9_-]{1,64}$/.test(data?.orderId??''),'Invalid payment reference.','invalid-argument');
    const p=await store.pending(uid,data.orderId);
    ensure(p && p.customerId===uid,'Payment does not belong to this user.','permission-denied');
    const existing=await store.completed(uid,data.orderId);
    if(existing) return existing;
    const receipt=await paypal.capture(data.orderId,requestId(uid,p.checkoutId,'capture'));
    return store.finish(p,verifiedCapture(receipt,p,merchantId));
  }
 };
}
