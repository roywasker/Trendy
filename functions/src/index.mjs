import {initializeApp} from 'firebase-admin/app';
import {getFirestore} from 'firebase-admin/firestore';
import {onCall,HttpsError} from 'firebase-functions/v2/https';
import {defineSecret,defineString} from 'firebase-functions/params';
import {paymentHandlers,PaymentError} from './payment.mjs';
import {firestoreStore} from './firestore-store.mjs';
import {paypalClient} from './paypal.mjs';
initializeApp();
const clientId=defineSecret('PAYPAL_CLIENT_ID'),secret=defineSecret('PAYPAL_SECRET');
const merchant=defineString('PAYPAL_MERCHANT_ID'),environment=defineString('PAYPAL_ENVIRONMENT',{default:'sandbox'});
const options={region:'us-central1',secrets:[clientId,secret],timeoutSeconds:120,maxInstances:5};
function callable(method) {
 return onCall(options,async request=>{
  if(!request.auth) throw new HttpsError('unauthenticated','Please sign in.');
  try {
   const handlers=paymentHandlers({store:firestoreStore(getFirestore()),paypal:paypalClient({clientId:clientId.value(),secret:secret.value(),environment:environment.value()}),merchantId:merchant.value()});
   return await handlers[method](request.auth.uid,request.data);
  } catch(error) {if(error instanceof PaymentError) throw new HttpsError(error.code,error.message);throw new HttpsError('unavailable','Unable to complete payment. Please retry confirmation.');}
 });
}
export const createPaypalCheckout=callable('create');
export const completePaypalCheckout=callable('complete');
