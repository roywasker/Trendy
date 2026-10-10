import { cents,usd,ensure,remainingCart } from './payment.mjs';
const canonicalCart=cart=>JSON.stringify([...cart].sort((a,b)=>a.id.localeCompare(b.id)).map(({id,productId,quantity,color})=>({id,productId,quantity,color:color??null})));
export function firestoreStore(db) {
 const customer=uid=>db.collection('customer').doc(uid);
 const checkout=(uid,id)=>db.collection('pendingCheckout').doc(`${uid}_${id}`);
 const mapping=id=>db.collection('paypalPending').doc(id);
 const order=id=>db.collection('order').doc(`paypal_${id}`);
 return {
  async prepare(uid,data) {
   return db.runTransaction(async tx=>{
    const ref=checkout(uid,data.checkoutId);
    const [pending,person,cod]=await Promise.all([tx.get(ref),tx.get(customer(uid)),tx.get(db.collection('order').doc(data.checkoutId))]);
    ensure(!cod.exists,'This checkout was already placed.');
    if(pending.exists) {
      const p=pending.data();
      ensure(canonicalCart(p.items)===canonicalCart(data.cart) && JSON.stringify(p.shipping)===JSON.stringify(data.shipping) && p.amount===data.expectedAmount,'Checkout reference already used.');
      return p;
    }
    ensure(person.exists && canonicalCart(person.data().cart??[])===canonicalCart(data.cart),'Your cart changed. Please review checkout.');
    const products=await Promise.all([...new Set(data.cart.map(i=>i.productId))].map(id=>tx.get(db.collection('product').doc(id))));
    const prices=new Map(products.map(p=>{ensure(p.exists,'A product is unavailable.');return [p.id,cents(p.data().price)];}));
    const total=data.cart.reduce((sum,i)=>sum+prices.get(i.productId)*i.quantity,0);
    ensure(total>0 && usd(total)===data.expectedAmount,'Prices changed. Please review checkout.');
    const p={customerId:uid,checkoutId:data.checkoutId,items:data.cart,shipping:data.shipping,amount:usd(total),unitPrices:Object.fromEntries(prices),createdAt:Date.now()};
    tx.create(ref,p); return p;
   });
  },
  async attach(uid,id,paypalId,approvalUrl) {
   return db.runTransaction(async tx=>{
    const ref=checkout(uid,id),doc=await tx.get(ref);
    ensure(doc.exists && (!doc.data().paypalId||doc.data().paypalId===paypalId),'Checkout reference conflict.');
    tx.update(ref,{paypalId,approvalUrl});tx.set(mapping(paypalId),{customerId:uid,checkoutId:id});
   });
  },
  async pending(uid,id) {
    const doc=await mapping(id).get();
    ensure(doc.exists && doc.data().customerId===uid,'Payment does not belong to this user.','permission-denied');
    return (await checkout(uid,doc.data().checkoutId).get()).data();
  },
  async completed(uid,id) {
    const doc=await order(id).get(); if(!doc.exists) return null;
    ensure(doc.data().customerId===uid,'Payment does not belong to this user.','permission-denied');return doc.data();
  },
  async finish(p,captureId) {
   return db.runTransaction(async tx=>{
    const [existing,person]=await Promise.all([tx.get(order(p.paypalId)),tx.get(customer(p.customerId))]);
    if(existing.exists) return existing.data();
    ensure(person.exists,'Customer is unavailable. Retry payment confirmation.');
    const placed={id:`paypal_${p.paypalId}`,customerId:p.customerId,items:p.items,totalAmount:Number(p.amount),token:p.paypalId,shipping:p.shipping};
    tx.create(order(p.paypalId),{...placed,captureId});
    tx.update(customer(p.customerId),{cart:remainingCart(person.data().cart??[],p.items)});
    tx.update(checkout(p.customerId,p.checkoutId),{status:'COMPLETED',captureId});return placed;
   });
  }
 };
}
