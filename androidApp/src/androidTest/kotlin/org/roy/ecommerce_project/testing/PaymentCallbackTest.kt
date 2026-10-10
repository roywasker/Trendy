package org.roy.ecommerce_project.testing

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.MapSettings
import com.russhwolf.settings.observable.makeObservable
import com.trendy.shared.util.PayPalCallback
import com.trendy.shared.util.PreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.roy.ecommerce_project.PayPalReturnIntent

@OptIn(ExperimentalSettingsApi::class)
@RunWith(AndroidJUnit4::class)
class PaymentCallbackTest {
    @Test fun successWithToken_isConfirmed() { val r = PayPalCallback.parse("true", null, "order-1"); assertEquals(true, r.isSuccess); assertNull(r.error); assertEquals("order-1", r.token) }
    @Test fun successWithoutToken_isRejected() { val r = PayPalCallback.parse("true", null, null); assertEquals(false, r.isSuccess); assertNotNull(r.error) }
    @Test fun successWithBlankToken_isRejected() { assertEquals(false, PayPalCallback.parse("true", null, " ").isSuccess) }
    @Test fun cancellationWithToken_isNeverSuccess() { val r = PayPalCallback.parse(null, "true", "order-1"); assertEquals(false, r.isSuccess); assertEquals("Payment has been canceled.", r.error) }
    @Test fun cancellationWithoutToken_isStillRecognized() { assertNotNull(PayPalCallback.parse(null, "true", null).error) }
    @Test fun cancellationOverridesConflictingSuccess() { assertEquals(false, PayPalCallback.parse("true", "true", "order-1").isSuccess) }
    @Test fun falseSuccess_isNotConfirmation() { assertEquals(false, PayPalCallback.parse("false", null, "order-1").isSuccess) }
    @Test fun malformedBoolean_isNotConfirmation() { assertEquals(false, PayPalCallback.parse("TRUE", "false", "order-1").isSuccess) }
    @Test fun missingFlags_isNotConfirmation() { assertEquals(false, PayPalCallback.parse(null, null, "order-1").isSuccess) }
    @Test fun falseCancellationDoesNotOverrideTrueSuccess() { assertEquals(true, PayPalCallback.parse("true", "false", "order-1").isSuccess) }
    @Test fun returnIntent_acceptsConfiguredSchemeAndDecodesToken() { val r = PayPalReturnIntent.parse(Uri.parse("com.example.trendy://paypalpay?success=true&token=order%201")); assertEquals("order 1", r?.token); assertEquals(true, r?.isSuccess) }
    @Test fun returnIntent_ignoresHttpsAndOtherSchemes() { assertNull(PayPalReturnIntent.parse(Uri.parse("https://example.test?success=true&token=x"))); assertNull(PayPalReturnIntent.parse(Uri.parse("another://return?success=true&token=x"))) }
    @Test fun returnIntent_ignoresNullUri() { assertNull(PayPalReturnIntent.parse(null)) }
    @Test fun returnIntent_ignoresOpaqueMalformedUri() { assertNull(PayPalReturnIntent.parse(Uri.parse("com.example.trendy:opaque"))) }
    @Test fun returnIntent_handlesCancellation() { assertEquals(false, PayPalReturnIntent.parse(Uri.parse("com.example.trendy://paypalpay?cancel=true&token=x"))?.isSuccess) }
    @Test fun emptyPreferences_emitNoPayment() = runBlocking { assertNull(PreferencesRepository(MapSettings().makeObservable()).readPayPalDataFlow().first()) }
    @Test fun preferences_roundTripSuccessfulPayment() = runBlocking {
        val prefs = PreferencesRepository(MapSettings().makeObservable()); prefs.savePayPalData(true, null, "order-1")
        val result = prefs.readPayPalDataFlow().first(); assertEquals(true, result?.isSuccess); assertEquals("order-1", result?.token); assertNull(result?.error)
    }
    @Test fun newCallback_cannotReuseOldTokenOrSuccess() = runBlocking {
        val prefs = PreferencesRepository(MapSettings().makeObservable()); prefs.savePayPalData(true, null, "old-order"); prefs.savePayPalData(null, "Canceled", null)
        val result = prefs.readPayPalDataFlow().first(); assertNull(result?.isSuccess); assertNull(result?.token); assertEquals("Canceled", result?.error)
    }
    @Test fun reset_removesPaymentButPreservesUnrelatedPreferences() = runBlocking {
        val settings = MapSettings().makeObservable(); settings.putString("unrelated", "keep")
        val prefs = PreferencesRepository(settings); prefs.savePayPalData(true, null, "order-1"); prefs.reset()
        assertNull(prefs.readPayPalDataFlow().first()); assertEquals("keep", settings.getString("unrelated", ""))
    }
    @Test fun preferenceInstances_areIndependent() = runBlocking {
        val a = PreferencesRepository(MapSettings().makeObservable()); val b = PreferencesRepository(MapSettings().makeObservable())
        a.savePayPalData(true, null, "a-order"); assertNull(b.readPayPalDataFlow().first())
    }
}
