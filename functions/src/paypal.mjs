import {ensure} from './payment.mjs';
export function paypalClient({clientId,secret,environment,fetcher=fetch}) {
 ensure(['sandbox','live'].includes(environment),'Invalid PayPal environment.');
 const base=environment==='live'?'https://api-m.paypal.com':'https://api-m.sandbox.paypal.com';
 async function request(path,body,id,method='POST') {
  const auth=await fetcher(`${base}/v1/oauth2/token`,{method:'POST',headers:{Authorization:`Basic ${Buffer.from(`${clientId}:${secret}`).toString('base64')}`,'Content-Type':'application/x-www-form-urlencoded'},body:'grant_type=client_credentials',signal:AbortSignal.timeout(20000)});
  ensure(auth.ok,'Payment service authentication failed.');const token=(await auth.json()).access_token;ensure(token,'Payment service authentication failed.');
  const response=await fetcher(`${base}${path}`,{method,headers:{Authorization:`Bearer ${token}`,'Content-Type':'application/json','PayPal-Request-Id':id,Prefer:'return=representation'},...(body?{body:JSON.stringify(body)}:{}),signal:AbortSignal.timeout(20000)});
  const result=await response.json();
  if(!response.ok && method==='POST' && path.endsWith('/capture') && result.details?.some(d=>d.issue==='ORDER_ALREADY_CAPTURED')) return request(path.slice(0,-8),null,id,'GET');
  ensure(response.ok,'PayPal did not complete the request. Please retry.');return result;
 }
 return {
  create(p,id) {
   const s=p.shipping;
   return request('/v2/checkout/orders',{intent:'CAPTURE',purchase_units:[{reference_id:p.checkoutId,amount:{currency_code:'USD',value:p.amount},shipping:{name:{full_name:s.fullName},address:{address_line_1:s.addressLine1,admin_area_2:s.city,admin_area_1:s.state,postal_code:s.postalCode,country_code:s.countryCode}}}],payment_source:{paypal:{experience_context:{shipping_preference:'SET_PROVIDED_ADDRESS',user_action:'PAY_NOW',return_url:'com.example.trendy://paypalpay?success=true',cancel_url:'com.example.trendy://paypalpay?cancel=true'}}}},id);
  },
  async capture(id,key) {
   const receipt=await request(`/v2/checkout/orders/${id}/capture`,{},key);
   // Minimal capture responses omit merchant/amount details; retrieve the full order before validation.
   return receipt.purchase_units?.[0]?.payee ? receipt : request(`/v2/checkout/orders/${id}`,null,key,'GET');
  }
 };
}
