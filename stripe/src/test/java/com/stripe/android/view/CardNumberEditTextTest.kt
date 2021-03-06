package com.stripe.android.view

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.stripe.android.CardNumberFixtures.VALID_AMEX_NO_SPACES
import com.stripe.android.CardNumberFixtures.VALID_AMEX_WITH_SPACES
import com.stripe.android.CardNumberFixtures.VALID_DINERS_CLUB_NO_SPACES
import com.stripe.android.CardNumberFixtures.VALID_DINERS_CLUB_WITH_SPACES
import com.stripe.android.CardNumberFixtures.VALID_VISA_NO_SPACES
import com.stripe.android.CardNumberFixtures.VALID_VISA_WITH_SPACES
import com.stripe.android.model.Card
import com.stripe.android.testharness.ViewTestUtils
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.clearInvocations
import org.mockito.Mockito.reset
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner

/**
 * Test class for [CardNumberEditText].
 */
@RunWith(RobolectricTestRunner::class)
class CardNumberEditTextTest {

    @Mock
    private lateinit var cardNumberCompleteListener: CardNumberEditText.CardNumberCompleteListener
    @Mock
    private lateinit var cardBrandChangeListener: CardNumberEditText.CardBrandChangeListener
    private lateinit var cardNumberEditText: CardNumberEditText

    @BeforeTest
    fun setup() {
        MockitoAnnotations.initMocks(this)

        cardNumberEditText = CardNumberEditText(
            ApplicationProvider.getApplicationContext<Context>()
        )
        cardNumberEditText.setText("")
        cardNumberEditText.setCardNumberCompleteListener(cardNumberCompleteListener)
        cardNumberEditText.setCardBrandChangeListener(cardBrandChangeListener)
    }

    @Test
    fun updateSelectionIndex_whenVisa_increasesIndexWhenGoingPastTheSpaces() {
        // Directly setting card brand as a testing hack (annotated in source)
        cardNumberEditText.cardBrand = Card.CardBrand.VISA

        // Adding 1 character, starting at position 4, with a final string length 6
        assertEquals(6, cardNumberEditText.updateSelectionIndex(6, 4, 1))
        assertEquals(6, cardNumberEditText.updateSelectionIndex(8, 4, 1))
        assertEquals(11, cardNumberEditText.updateSelectionIndex(11, 9, 1))
        assertEquals(16, cardNumberEditText.updateSelectionIndex(16, 14, 1))
    }

    @Test
    fun updateSelectionIndex_whenAmEx_increasesIndexWhenGoingPastTheSpaces() {
        cardNumberEditText.cardBrand = Card.CardBrand.AMERICAN_EXPRESS
        assertEquals(6, cardNumberEditText.updateSelectionIndex(6, 4, 1))
        assertEquals(13, cardNumberEditText.updateSelectionIndex(13, 11, 1))
    }

    @Test
    fun updateSelectionIndex_whenDinersClub_decreasesIndexWhenDeletingPastTheSpaces() {
        cardNumberEditText.cardBrand = Card.CardBrand.DINERS_CLUB
        assertEquals(4, cardNumberEditText.updateSelectionIndex(6, 5, 0))
        assertEquals(9, cardNumberEditText.updateSelectionIndex(13, 10, 0))
        assertEquals(14, cardNumberEditText.updateSelectionIndex(17, 15, 0))
    }

    @Test
    fun updateSelectionIndex_whenDeletingNotOnGaps_doesNotDecreaseIndex() {
        cardNumberEditText.cardBrand = Card.CardBrand.DINERS_CLUB
        assertEquals(7, cardNumberEditText.updateSelectionIndex(12, 7, 0))
    }

    @Test
    fun updateSelectionIndex_whenAmEx_decreasesIndexWhenDeletingPastTheSpaces() {
        cardNumberEditText.cardBrand = Card.CardBrand.AMERICAN_EXPRESS
        assertEquals(4, cardNumberEditText.updateSelectionIndex(10, 5, 0))
        assertEquals(11, cardNumberEditText.updateSelectionIndex(13, 12, 0))
    }

    @Test
    fun updateSelectionIndex_whenSelectionInTheMiddle_increasesIndexOverASpace() {
        cardNumberEditText.cardBrand = Card.CardBrand.VISA
        assertEquals(6, cardNumberEditText.updateSelectionIndex(10, 4, 1))
    }

    @Test
    fun updateSelectionIndex_whenPastingIntoAGap_includesTheGapJump() {
        cardNumberEditText.cardBrand = Card.CardBrand.UNKNOWN
        assertEquals(11, cardNumberEditText.updateSelectionIndex(12, 8, 2))
    }

    @Test
    fun updateSelectionIndex_whenPastingOverAGap_includesTheGapJump() {
        cardNumberEditText.cardBrand = Card.CardBrand.UNKNOWN
        assertEquals(9, cardNumberEditText.updateSelectionIndex(12, 3, 5))
    }

    @Test
    fun updateSelectionIndex_whenIndexWouldGoOutOfBounds_setsToEndOfString() {
        cardNumberEditText.cardBrand = Card.CardBrand.VISA
        // This case could happen when you paste over 5 digits with only 2
        assertEquals(3, cardNumberEditText.updateSelectionIndex(3, 3, 2))
    }

    @Test
    fun setText_whenTextIsValidCommonLengthNumber_changesCardValidState() {
        cardNumberEditText.setText(VALID_VISA_WITH_SPACES)

        assertTrue(cardNumberEditText.isCardNumberValid)
        verify(cardNumberCompleteListener).onCardNumberComplete()
    }

    @Test
    fun setText_whenTextIsSpacelessValidNumber_changesToSpaceNumberAndValidates() {
        cardNumberEditText.setText(VALID_VISA_NO_SPACES)

        assertTrue(cardNumberEditText.isCardNumberValid)
        verify(cardNumberCompleteListener).onCardNumberComplete()
    }

    @Test
    fun setText_whenTextIsValidAmExDinersClubLengthNumber_changesCardValidState() {
        cardNumberEditText.setText(VALID_AMEX_WITH_SPACES)

        assertTrue(cardNumberEditText.isCardNumberValid)
        verify(cardNumberCompleteListener).onCardNumberComplete()
    }

    @Test
    fun setText_whenTextChangesFromValidToInvalid_changesCardValidState() {
        cardNumberEditText.setText(VALID_VISA_WITH_SPACES)
        // Simply setting the value interacts with this mock once -- that is tested elsewhere
        reset(cardNumberCompleteListener)

        var mutable = cardNumberEditText.text.toString()
        // Removing a single character should make this invalid
        mutable = mutable.substring(0, 18)
        cardNumberEditText.setText(mutable)
        assertFalse(cardNumberEditText.isCardNumberValid)
        verifyNoMoreInteractions(cardNumberCompleteListener)
    }

    @Test
    fun setText_whenTextIsInvalidCommonLengthNumber_doesNotNotifyListener() {
        // This creates a full-length but not valid number: 4242 4242 4242 4243
        val almostValid = VALID_VISA_WITH_SPACES.substring(0, 18) + "3"
        cardNumberEditText.setText(almostValid)
        assertFalse(cardNumberEditText.isCardNumberValid)
        verifyNoMoreInteractions(cardNumberCompleteListener)
    }

    @Test
    fun whenNotFinishedTyping_doesNotSetErrorValue() {
        // We definitely shouldn't start out in an error state.
        assertFalse(cardNumberEditText.shouldShowError)

        cardNumberEditText.setText(cardNumberEditText.text?.toString() + "123")
        assertFalse(cardNumberEditText.shouldShowError)
    }

    @Test
    fun finishTypingCommonLengthCardNumber_whenValidCard_doesNotSetErrorValue() {
        val almostThere = VALID_VISA_WITH_SPACES.substring(0, 18)
        cardNumberEditText.setText(almostThere)
        assertFalse(cardNumberEditText.shouldShowError)
        // We now have the valid 4242 Visa
        cardNumberEditText.setText(cardNumberEditText.text?.toString() + "2")
        assertFalse(cardNumberEditText.shouldShowError)
    }

    @Test
    fun finishTypingCommonLengthCardNumber_whenInvalidCard_setsErrorValue() {
        val almostThere = VALID_VISA_WITH_SPACES.substring(0, 18)
        cardNumberEditText.setText(almostThere)
        // This makes the number officially invalid
        cardNumberEditText.setText(cardNumberEditText.text?.toString() + "3")
        assertTrue(cardNumberEditText.shouldShowError)
    }

    @Test
    fun finishTypingInvalidCardNumber_whenFollowedByDelete_setsErrorBackToFalse() {
        val notQuiteValid = VALID_VISA_WITH_SPACES.substring(0, 18) + "3"
        cardNumberEditText.setText(notQuiteValid)
        assertTrue(cardNumberEditText.shouldShowError)

        // Now that we're in an error state, back up by one
        ViewTestUtils.sendDeleteKeyEvent(cardNumberEditText)
        assertFalse(cardNumberEditText.shouldShowError)
    }

    @Test
    fun finishTypingDinersClub_whenInvalid_setsErrorValueAndRemovesItAppropriately() {
        val notQuiteValid = VALID_DINERS_CLUB_WITH_SPACES.substring(0, 16) + "3"
        cardNumberEditText.setText(notQuiteValid)
        assertTrue(cardNumberEditText.shouldShowError)

        // Now that we're in an error state, back up by one
        ViewTestUtils.sendDeleteKeyEvent(cardNumberEditText)
        assertFalse(cardNumberEditText.shouldShowError)

        cardNumberEditText.setText(cardNumberEditText.text?.toString() + "4")
        assertFalse(cardNumberEditText.shouldShowError)
    }

    @Test
    fun finishTypingAmEx_whenInvalid_setsErrorValueAndRemovesItAppropriately() {
        val notQuiteValid = VALID_AMEX_WITH_SPACES.substring(0, 16) + "3"
        cardNumberEditText.setText(notQuiteValid)
        assertTrue(cardNumberEditText.shouldShowError)

        // Now that we're in an error state, back up by one
        ViewTestUtils.sendDeleteKeyEvent(cardNumberEditText)
        assertFalse(cardNumberEditText.shouldShowError)

        cardNumberEditText.setText(cardNumberEditText.text?.toString() + "5")
        assertFalse(cardNumberEditText.shouldShowError)
    }

    @Test
    fun setCardBrandChangeListener_callsSetCardBrand() {
        verify(cardBrandChangeListener)
            .onCardBrandChanged(Card.CardBrand.UNKNOWN)
    }

    @Test
    fun addVisaPrefix_callsBrandListener() {
        // We reset because just attaching the listener calls the method once.
        clearInvocations(cardBrandChangeListener)
        // There is only one Visa Prefix.
        cardNumberEditText.setText(cardNumberEditText.text?.toString() + Card.PREFIXES_VISA[0])
        verify(cardBrandChangeListener)
            .onCardBrandChanged(Card.CardBrand.VISA)
    }

    @Test
    fun addAmExPrefix_callsBrandListener() {
        Card.PREFIXES_AMERICAN_EXPRESS.forEach { prefix ->
            // Reset inside the loop so we don't count each prefix
            clearInvocations(cardBrandChangeListener)
            cardNumberEditText.setText(cardNumberEditText.text?.toString() + prefix)
            verify(cardBrandChangeListener).onCardBrandChanged(Card.CardBrand.AMERICAN_EXPRESS)
            cardNumberEditText.setText("")
        }
    }

    @Test
    fun addDinersClubPrefix_callsBrandListener() {
        Card.PREFIXES_DINERS_CLUB.forEach { prefix ->
            clearInvocations(cardBrandChangeListener)
            cardNumberEditText.setText(cardNumberEditText.text?.toString() + prefix)
            verify(cardBrandChangeListener).onCardBrandChanged(Card.CardBrand.DINERS_CLUB)
            cardNumberEditText.setText("")
        }
    }

    @Test
    fun addDiscoverPrefix_callsBrandListener() {
        Card.PREFIXES_DISCOVER.forEach { prefix ->
            clearInvocations(cardBrandChangeListener)
            cardNumberEditText.setText(cardNumberEditText.text?.toString() + prefix)
            verify(cardBrandChangeListener).onCardBrandChanged(Card.CardBrand.DISCOVER)
            cardNumberEditText.setText("")
        }
    }

    @Test
    fun addMasterCardPrefix_callsBrandListener() {
        Card.PREFIXES_MASTERCARD.forEach { prefix ->
            clearInvocations(cardBrandChangeListener)
            cardNumberEditText.setText(cardNumberEditText.text?.toString() + prefix)
            verify(cardBrandChangeListener).onCardBrandChanged(Card.CardBrand.MASTERCARD)
            cardNumberEditText.setText("")
        }
    }

    @Test
    fun addJCBPrefix_callsBrandListener() {
        Card.PREFIXES_JCB.forEach { prefix ->
            clearInvocations(cardBrandChangeListener)
            cardNumberEditText.setText(cardNumberEditText.text?.toString() + prefix)
            verify(cardBrandChangeListener).onCardBrandChanged(Card.CardBrand.JCB)
            cardNumberEditText.setText("")
        }
    }

    @Test
    fun enterCompleteNumberInParts_onlyCallsBrandListenerOnce() {
        clearInvocations(cardBrandChangeListener)
        val prefix = VALID_AMEX_WITH_SPACES.substring(0, 2)
        val suffix = VALID_AMEX_WITH_SPACES.substring(2)
        cardNumberEditText.setText(cardNumberEditText.text?.toString() + prefix)
        cardNumberEditText.setText(cardNumberEditText.text?.toString() + suffix)
        verify(cardBrandChangeListener).onCardBrandChanged(Card.CardBrand.AMERICAN_EXPRESS)
    }

    @Test
    fun enterBrandPrefix_thenDelete_callsUpdateWithUnknown() {
        clearInvocations(cardBrandChangeListener)
        val dinersPrefix = Card.PREFIXES_DINERS_CLUB[0]
        // All the Diners Club prefixes are longer than one character, but I specifically want
        // to test that a nonempty string still triggers this action, so this test will fail if
        // the Diners Club prefixes are ever changed.
        assertTrue(dinersPrefix.length > 1)

        cardNumberEditText.setText(cardNumberEditText.text?.toString() + dinersPrefix)
        verify(cardBrandChangeListener).onCardBrandChanged(Card.CardBrand.DINERS_CLUB)

        ViewTestUtils.sendDeleteKeyEvent(cardNumberEditText)
        verify(cardBrandChangeListener).onCardBrandChanged(Card.CardBrand.UNKNOWN)
    }

    @Test
    fun enterBrandPrefix_thenClearAllText_callsUpdateWithUnknown() {
        clearInvocations(cardBrandChangeListener)
        val prefixVisa = Card.PREFIXES_VISA[0]
        cardNumberEditText.setText(cardNumberEditText.text?.toString() + prefixVisa)
        verify(cardBrandChangeListener)
            .onCardBrandChanged(Card.CardBrand.VISA)

        // Just adding some other text. Not enough to invalidate the card or complete it.
        cardNumberEditText.setText(cardNumberEditText.text?.toString() + "123")
        verify(cardBrandChangeListener, never())
            .onCardBrandChanged(Card.CardBrand.UNKNOWN)

        // This simulates the user selecting all text and deleting it.
        cardNumberEditText.setText("")

        verify(cardBrandChangeListener).onCardBrandChanged(Card.CardBrand.UNKNOWN)
    }

    @Test
    fun getCardNumber_whenValidCard_returnsCardNumberWithoutSpaces() {
        cardNumberEditText.setText(VALID_VISA_WITH_SPACES)
        assertEquals(VALID_VISA_NO_SPACES, cardNumberEditText.cardNumber)

        cardNumberEditText.setText(VALID_AMEX_WITH_SPACES)
        assertEquals(VALID_AMEX_NO_SPACES, cardNumberEditText.cardNumber)

        cardNumberEditText.setText(VALID_DINERS_CLUB_WITH_SPACES)
        assertEquals(VALID_DINERS_CLUB_NO_SPACES, cardNumberEditText.cardNumber)
    }

    @Test
    fun getCardNumber_whenIncompleteCard_returnsNull() {
        cardNumberEditText.setText(
            VALID_DINERS_CLUB_WITH_SPACES
                .substring(0, VALID_DINERS_CLUB_WITH_SPACES.length - 2))
        assertNull(cardNumberEditText.cardNumber)
    }

    @Test
    fun getCardNumber_whenInvalidCardNumber_returnsNull() {
        var almostVisa = VALID_VISA_WITH_SPACES.substring(0, VALID_VISA_WITH_SPACES.length - 1)
        almostVisa += "3" // creates the 4242 4242 4242 4243 bad number
        cardNumberEditText.setText(almostVisa)
        assertNull(cardNumberEditText.cardNumber)
    }

    @Test
    fun getCardNumber_whenValidNumberIsChangedToInvalid_returnsNull() {
        cardNumberEditText.setText(VALID_AMEX_WITH_SPACES)
        ViewTestUtils.sendDeleteKeyEvent(cardNumberEditText)

        assertNull(cardNumberEditText.cardNumber)
    }

    @Test
    fun createFormattedNumber_whenCardsPartsHasNull_excludesNullAndAfter() {
        assertEquals(
            "4242 4242",
            CardNumberEditText.createFormattedNumber(arrayOf("4242", "4242", null, "4242"))
        )
    }
}
