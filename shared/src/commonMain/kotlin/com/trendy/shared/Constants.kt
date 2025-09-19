package com.trendy.shared

object Constants {

    const val WEB_CLIENT_ID = "151385642901-613at8itm99estj1kei9e0a1qo9aff9p.apps.googleusercontent.com"

    const val MAX_QUANTITY = 10
    const val MIN_QUANTITY = 1

    const val PAYPAL_CLIENT_ID = "ASsQG7k-uAHevHW6Wqae8EH9ZvNgtQPv62zigyV4H-haARKFOQRo6uMkG1vY-7uceLphq-DwN_qmUhPD"
    const val PAYPAL_SECRET_ID = "EBCcR1B3XCqHuMhx1eMGFYqpDGPizbIU3mkzRzAsKqnj6Movmbf7cv8pqTVGbUEJgQsbXPcq9nXzu1qa"

    const val PAYPAL_AUTH_KEY = "${PAYPAL_CLIENT_ID}:${PAYPAL_SECRET_ID}"
    const val PAYPAL_AUTH_ENDPOINT = "https://api-m.sandbox.paypal.com/v1/oauth2/token"
    const val PAYPAL_CHECKOUT_ENDPOINT = "https://api-m.sandbox.paypal.com/v2/checkout/orders"

    const val RETURN_URL = "com.stevdza.san.nutrisport://paypalpay?success=true"
    const val CANCEL_URL = "com.stevdza.san.nutrisport://paypalpay?cancel=true"
}