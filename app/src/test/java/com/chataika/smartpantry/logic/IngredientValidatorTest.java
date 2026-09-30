package com.chataika.smartpantry.logic;

import static org.junit.Assert.assertEquals;

import com.chataika.smartpantry.logic.IngredientValidator.NameError;
import com.chataika.smartpantry.logic.IngredientValidator.QuantityError;

import org.junit.Test;

public class IngredientValidatorTest {

    @Test
    public void names() {
        assertEquals(NameError.NONE, IngredientValidator.validateName("Tomatoes"));
        assertEquals(NameError.NONE, IngredientValidator.validateName("  spring onion "));
        assertEquals(NameError.NONE, IngredientValidator.validateName("crème fraîche"));
        assertEquals(NameError.REQUIRED, IngredientValidator.validateName("   "));
        assertEquals(NameError.REQUIRED, IngredientValidator.validateName(null));
        assertEquals(NameError.LENGTH, IngredientValidator.validateName("a"));
        assertEquals(NameError.INVALID_CHARS, IngredientValidator.validateName("egg123"));
        assertEquals(NameError.INVALID_CHARS, IngredientValidator.validateName("-egg"));
    }

    @Test
    public void quantities() {
        assertEquals(QuantityError.NONE, IngredientValidator.validateQuantity("2"));
        assertEquals(QuantityError.NONE, IngredientValidator.validateQuantity("0.5"));
        assertEquals(QuantityError.NONE, IngredientValidator.validateQuantity("1,5"));
        assertEquals(QuantityError.REQUIRED, IngredientValidator.validateQuantity(""));
        assertEquals(QuantityError.INVALID, IngredientValidator.validateQuantity("0"));
        assertEquals(QuantityError.INVALID, IngredientValidator.validateQuantity("-3"));
        assertEquals(QuantityError.INVALID, IngredientValidator.validateQuantity("abc"));
        assertEquals(QuantityError.INVALID, IngredientValidator.validateQuantity("NaN"));
        assertEquals(QuantityError.TOO_LARGE, IngredientValidator.validateQuantity("1000000"));
        assertEquals(1.5, IngredientValidator.parseQuantity("1,5"), 1e-9);
    }
}
