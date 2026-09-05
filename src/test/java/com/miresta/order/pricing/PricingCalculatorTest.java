package com.miresta.order.pricing;

import com.miresta.catalog.Category;
import com.miresta.catalog.Product;
import com.miresta.order.OrderItemSelection;
import com.miresta.shared.ComboCategory;
import com.miresta.shared.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PricingCalculatorTest {

    private final Map<PriceCode, Long> seed = new EnumMap<>(PriceCode.class);
    private PricingCalculator calculator;

    @BeforeEach
    void setUp() {
        seed.put(PriceCode.DESAYUNO_COMPLETO, 8000L);
        seed.put(PriceCode.DESAYUNO_BANDEJA, 7000L);
        seed.put(PriceCode.DESAYUNO_ACOMPANANTE_ADICION, 1000L);
        seed.put(PriceCode.DESAYUNO_PROTEINA_ADICION, 4000L);

        seed.put(PriceCode.ALMUERZO_COMPLETO, 10000L);
        seed.put(PriceCode.ALMUERZO_BANDEJA, 9000L);
        seed.put(PriceCode.ALMUERZO_ACOMPANANTE_ADICION, 1000L);
        seed.put(PriceCode.ALMUERZO_PROTEINA_ADICION, 5000L);
        seed.put(PriceCode.ALMUERZO_COMPONENTE_SUELTO, 1000L);
        seed.put(PriceCode.ALMUERZO_SOPA_SUELTA, 5000L);
        seed.put(PriceCode.ALMUERZO_PROTEINA_SUELTA, 5000L);

        seed.put(PriceCode.ESPECIAL_COMPLETO, 25000L);
        seed.put(PriceCode.ESPECIAL_PROTEINA_ADICION, 12000L);

        // The two container costs para llevar — global, not per meal type.
        seed.put(PriceCode.ENVASE_SOPA, 1000L);
        seed.put(PriceCode.ENVASE_BANDEJA, 1000L);

        PriceSettingService priceSettings = mock(PriceSettingService.class);
        when(priceSettings.amountFor(any())).thenAnswer(inv -> Money.of(seed.get((PriceCode) inv.getArgument(0))));

        calculator = new PricingCalculator(priceSettings);
    }

    private Category category(ComboCategory code) {
        Category category = new Category();
        category.setName(code.name());
        category.setCode(code);
        return category;
    }

    private Product product(String name, ComboCategory categoryCode) {
        Product product = new Product();
        product.setName(name);
        product.setCategory(category(categoryCode));
        return product;
    }

    private OrderItemSelection selection(Product product, long quantity) {
        OrderItemSelection selection = new OrderItemSelection();
        selection.setProduct(product);
        selection.setQuantity(quantity);
        selection.setUnitExtraPrice(Money.ZERO);
        return selection;
    }

    @Test
    void almuerzoCompleto_enSitio() {
        var selections = List.of(
                selection(product("Sopa de verduras", ComboCategory.SOPA), 1),
                selection(product("Arroz", ComboCategory.ACOMPANANTE), 1),
                selection(product("Ensalada", ComboCategory.ACOMPANANTE), 1),
                selection(product("Pollo", ComboCategory.PROTEINA), 1)
        );

        PricedOrderItem priced = calculator.priceOrderItem("ALMUERZO", false, selections);

        assertThat(priced.total().amount()).isEqualTo(10000);
        assertThat(priced.baseTotal().amount()).isEqualTo(10000);
        assertThat(priced.toGoSurcharge().amount()).isZero();
    }

    @Test
    void almuerzoCompleto_paraLlevar_dosEnvases() {
        // Completo needs BOTH containers: one for the soup, one for the tray.
        var selections = List.of(
                selection(product("Sopa de verduras", ComboCategory.SOPA), 1),
                selection(product("Arroz", ComboCategory.ACOMPANANTE), 1),
                selection(product("Ensalada", ComboCategory.ACOMPANANTE), 1),
                selection(product("Pollo", ComboCategory.PROTEINA), 1)
        );

        PricedOrderItem priced = calculator.priceOrderItem("ALMUERZO", true, selections);

        assertThat(priced.baseTotal().amount()).isEqualTo(10000);
        assertThat(priced.toGoSurcharge().amount()).isEqualTo(2000);
        assertThat(priced.total().amount()).isEqualTo(12000);
    }

    @Test
    void almuerzoBandeja_paraLlevar_sinSopa_unSoloEnvase() {
        var selections = List.of(
                selection(product("Arroz", ComboCategory.ACOMPANANTE), 1),
                selection(product("Ensalada", ComboCategory.ACOMPANANTE), 1),
                selection(product("Pollo", ComboCategory.PROTEINA), 1)
        );

        PricedOrderItem priced = calculator.priceOrderItem("ALMUERZO", true, selections);

        // No soup on this order — only the tray container applies.
        assertThat(priced.baseTotal().amount()).isEqualTo(9000);
        assertThat(priced.toGoSurcharge().amount()).isEqualTo(1000);
        assertThat(priced.total().amount()).isEqualTo(10000);
    }

    @Test
    void desayunoCompleto_paraLlevar_dosEnvases() {
        // Regression: under the old flat "amount to go per combo" model, desayuno's to-go
        // delta was hand-set to a flat +1000 regardless of composition — wrong, since a
        // completo needs both containers just like almuerzo. Now it's derived, not hand-set.
        var selections = List.of(
                selection(product("Caldo", ComboCategory.SOPA), 1),
                selection(product("Arroz", ComboCategory.ACOMPANANTE), 1),
                selection(product("Ensalada", ComboCategory.ACOMPANANTE), 1),
                selection(product("Pollo", ComboCategory.PROTEINA), 1)
        );

        PricedOrderItem priced = calculator.priceOrderItem("DESAYUNO", true, selections);

        assertThat(priced.baseTotal().amount()).isEqualTo(8000);
        assertThat(priced.toGoSurcharge().amount()).isEqualTo(2000);
        assertThat(priced.total().amount()).isEqualTo(10000);
    }

    @Test
    void almuerzo_principioCuentaComoAcompanante_paraElUmbral() {
        // Regression: principio used to NOT count toward the "2 of 4" lunch threshold.
        var selections = List.of(
                selection(product("Sopa", ComboCategory.SOPA), 1),
                selection(product("Papa criolla", ComboCategory.PRINCIPIO), 1),
                selection(product("Arroz", ComboCategory.ACOMPANANTE), 1),
                selection(product("Pollo", ComboCategory.PROTEINA), 1)
        );

        PricedOrderItem priced = calculator.priceOrderItem("ALMUERZO", false, selections);

        assertThat(priced.baseTotal().amount()).isEqualTo(10000);
    }

    @Test
    void especialDomingo_precioFijo_sinImportarBandejaOCompleto() {
        var completo = List.of(
                selection(product("Sancocho", ComboCategory.SOPA), 1),
                selection(product("Pollo", ComboCategory.PROTEINA), 1)
        );

        PricedOrderItem priced = calculator.priceOrderItem("ESPECIAL", false, completo);

        assertThat(priced.total().amount()).isEqualTo(25000);
    }

    @Test
    void especial_soloSopa_paraLlevar_unSoloEnvase() {
        var selections = List.of(
                selection(product("Sancocho", ComboCategory.SOPA), 1)
        );

        PricedOrderItem priced = calculator.priceOrderItem("ESPECIAL", true, selections);

        // No protein/tray on this order — only the soup container applies.
        assertThat(priced.baseTotal().amount()).isEqualTo(25000);
        assertThat(priced.toGoSurcharge().amount()).isEqualTo(1000);
        assertThat(priced.total().amount()).isEqualTo(26000);
    }

    @Test
    void especial_sopaYProteina_paraLlevar_dosEnvases() {
        var selections = List.of(
                selection(product("Sancocho", ComboCategory.SOPA), 1),
                selection(product("Pollo", ComboCategory.PROTEINA), 1)
        );

        PricedOrderItem priced = calculator.priceOrderItem("ESPECIAL", true, selections);

        assertThat(priced.baseTotal().amount()).isEqualTo(25000);
        assertThat(priced.toGoSurcharge().amount()).isEqualTo(2000);
        assertThat(priced.total().amount()).isEqualTo(27000);
    }

    @Test
    void especial_platoSolo_cobraPrecioFijo() {
        // Reported live bug: a product in the ESPECIAL catalog category (Bandeja
        // paisa, Sancocho de gallina, etc.) IS the whole plate — ordered by itself, with
        // nothing else, it used to fall through to the loose-component price ($12.000)
        // instead of the fixed especial price ($25.000).
        var selections = List.of(
                selection(product("Bandeja paisa", ComboCategory.ESPECIAL), 1)
        );

        PricedOrderItem priced = calculator.priceOrderItem("ESPECIAL", false, selections);

        assertThat(priced.baseTotal().amount()).isEqualTo(25000);
        assertThat(priced.total().amount()).isEqualTo(25000);
    }

    @Test
    void especial_dosPlatos_cobraElDoble() {
        // Unlike the other meal types, each ESPECIAL unit is its own standalone plate —
        // 2x Bandeja paisa is two full meals, not one combo with "extra".
        var selections = List.of(
                selection(product("Bandeja paisa", ComboCategory.ESPECIAL), 2)
        );

        PricedOrderItem priced = calculator.priceOrderItem("ESPECIAL", false, selections);

        assertThat(priced.baseTotal().amount()).isEqualTo(50000);
        assertThat(priced.total().amount()).isEqualTo(50000);
    }

    @Test
    void huevoReemplazaPrincipio_sinCobroExtra() {
        Product huevo = product("Huevo frito", ComboCategory.ADICIONAL);

        var selections = List.of(
                selection(product("Sopa", ComboCategory.SOPA), 1),
                selection(product("Arroz", ComboCategory.ACOMPANANTE), 1),
                selection(product("Pollo", ComboCategory.PROTEINA), 1)
        );
        var huevoSelection = selection(huevo, 1);
        huevoSelection.setReplacementCategory(ComboCategory.PRINCIPIO);

        var all = new java.util.ArrayList<>(selections);
        all.add(huevoSelection);

        PricedOrderItem priced = calculator.priceOrderItem("ALMUERZO", false, all);

        // Egg-as-principio is free: total should equal the plain combo price, no adicional charge.
        assertThat(priced.total().amount()).isEqualTo(10000);
    }

    @Test
    void segundaProteina_cobraAdicionSegunComida() {
        var selections = List.of(
                selection(product("Sopa", ComboCategory.SOPA), 1),
                selection(product("Arroz", ComboCategory.ACOMPANANTE), 1),
                selection(product("Ensalada", ComboCategory.ACOMPANANTE), 1),
                selection(product("Pollo", ComboCategory.PROTEINA), 1),
                selection(product("Carne de res", ComboCategory.PROTEINA), 1)
        );

        PricedOrderItem priced = calculator.priceOrderItem("ALMUERZO", false, selections);

        assertThat(priced.total().amount()).isEqualTo(10000 + 5000);
    }

    @Test
    void sinComboMinimo_seCobraPorComponentesSueltos() {
        // Just one accompaniment, no protein/soup at all — never reaches combo pricing.
        var selections = List.of(
                selection(product("Maduro", ComboCategory.ACOMPANANTE), 1)
        );

        PricedOrderItem priced = calculator.priceOrderItem("ALMUERZO", true, selections);

        // Confirmed rule: any loose accompaniment is 1000, and to-go doesn't change it
        // (no combo formed here, so no container is owed either).
        assertThat(priced.baseTotal().amount()).isZero();
        assertThat(priced.total().amount()).isEqualTo(1000);
        assertThat(priced.toGoSurcharge().amount()).isZero();
    }

    @Test
    void soloSopa_paraLlevar_siLlevaEnvase() {
        // Regression: a lone sopa taken to go still needs its own container — a prior
        // version only charged the envase when a full combo formed, so "solo sopa" para
        // llevar was missing the surcharge entirely (reported as a live bug).
        var selections = List.of(
                selection(product("Sopa de maíz", ComboCategory.SOPA), 1)
        );

        PricedOrderItem priced = calculator.priceOrderItem("ALMUERZO", true, selections);

        assertThat(priced.baseTotal().amount()).isZero();
        assertThat(priced.individualsTotal().amount()).isEqualTo(5000);
        assertThat(priced.toGoSurcharge().amount()).isEqualTo(1000);
        assertThat(priced.total().amount()).isEqualTo(6000);
    }

    @Test
    void almuerzoCompleto_principioMixto_segundoPrincipioComoAdicional() {
        // "Principio mixto" — dos principios distintos en vez de un principio + un
        // acompañante — sigue formando el combo completo (hay sopa), pero el segundo
        // principio se cobra como adición, igual que una segunda proteína.
        var selections = List.of(
                selection(product("Sopa", ComboCategory.SOPA), 1),
                selection(product("Papa criolla", ComboCategory.PRINCIPIO), 1),
                selection(product("Yuca", ComboCategory.PRINCIPIO), 1),
                selection(product("Pollo", ComboCategory.PROTEINA), 1)
        );

        PricedOrderItem priced = calculator.priceOrderItem("ALMUERZO", false, selections);

        assertThat(priced.baseTotal().amount()).isEqualTo(10000);
        assertThat(priced.sideAdditionalsTotal().amount()).isEqualTo(1000);
        assertThat(priced.total().amount()).isEqualTo(11000);
    }

    @Test
    void almuerzoBandeja_principioMixto_segundoPrincipioComoAdicional() {
        // Sin sopa — bandeja — y también principio mixto: el segundo principio se
        // cobra igual como adición; nunca actúa como reemplazo/sustituto de la sopa.
        var selections = List.of(
                selection(product("Papa criolla", ComboCategory.PRINCIPIO), 1),
                selection(product("Yuca", ComboCategory.PRINCIPIO), 1),
                selection(product("Pollo", ComboCategory.PROTEINA), 1)
        );

        PricedOrderItem priced = calculator.priceOrderItem("ALMUERZO", false, selections);

        assertThat(priced.baseTotal().amount()).isEqualTo(9000);
        assertThat(priced.sideAdditionalsTotal().amount()).isEqualTo(1000);
        assertThat(priced.total().amount()).isEqualTo(10000);
    }

    @Test
    void soloProteina_paraLlevar_siLlevaEnvase() {
        var selections = List.of(
                selection(product("Pollo", ComboCategory.PROTEINA), 1)
        );

        PricedOrderItem priced = calculator.priceOrderItem("ALMUERZO", true, selections);

        assertThat(priced.baseTotal().amount()).isZero();
        assertThat(priced.individualsTotal().amount()).isEqualTo(5000);
        assertThat(priced.toGoSurcharge().amount()).isEqualTo(1000);
        assertThat(priced.total().amount()).isEqualTo(6000);
    }

    @Test
    void muchasSopasSueltas_paraLlevar_envaseProporcional() {
        // Reported live bug: 20 sopas sueltas para llevar only got charged ONE envase
        // (a flat "¿hay al menos una sopa?" check) instead of one per soup actually going
        // out — each soup is its own liquid container.
        var selections = List.of(
                selection(product("Sopa de maíz", ComboCategory.SOPA), 20)
        );

        PricedOrderItem priced = calculator.priceOrderItem("ALMUERZO", true, selections);

        assertThat(priced.individualsTotal().amount()).isEqualTo(20 * 5000);
        assertThat(priced.toGoSurcharge().amount()).isEqualTo(20 * 1000);
        assertThat(priced.total().amount()).isEqualTo(20 * 5000 + 20 * 1000);
    }

    @Test
    void almuerzoCompleto_proteinaMixta_paraLlevar_unSoloEnvaseBandeja() {
        // Confirmed rule: unlike the soup container, the tray container stays flat at one
        // regardless of how many proteins are on the plate — it's still one physical box.
        var selections = List.of(
                selection(product("Sopa", ComboCategory.SOPA), 1),
                selection(product("Arroz", ComboCategory.ACOMPANANTE), 1),
                selection(product("Ensalada", ComboCategory.ACOMPANANTE), 1),
                selection(product("Pollo", ComboCategory.PROTEINA), 1),
                selection(product("Carne de res", ComboCategory.PROTEINA), 1)
        );

        PricedOrderItem priced = calculator.priceOrderItem("ALMUERZO", true, selections);

        assertThat(priced.baseTotal().amount()).isEqualTo(10000);
        assertThat(priced.proteinAdditionalsTotal().amount()).isEqualTo(5000);
        // One envase for the soup, one flat envase for the tray — not two for the tray.
        assertThat(priced.toGoSurcharge().amount()).isEqualTo(2000);
        assertThat(priced.total().amount()).isEqualTo(10000 + 5000 + 2000);
    }

    @Test
    void principioPorSopa_solo_paraLlevar_cobraComoSopaSuelta() {
        // Reported live bug: a principio replacing the soup ("por sopa"), ordered alone
        // with nothing else, priced as a generic loose accompaniment ($1.000) instead of
        // as the soup it's standing in for — and charged no envase at all, since the
        // replacement deliberately doesn't count toward combo pricing. It should still
        // price (and need a container) exactly like a real sopa suelta would.
        var frijoles = selection(product("Frijoles", ComboCategory.PRINCIPIO), 1);
        frijoles.setReplacementCategory(ComboCategory.SOPA);

        PricedOrderItem priced = calculator.priceOrderItem("ALMUERZO", true, List.of(frijoles));

        assertThat(priced.baseTotal().amount()).isZero();
        assertThat(priced.individualsTotal().amount()).isEqualTo(5000);
        assertThat(priced.toGoSurcharge().amount()).isEqualTo(1000);
        assertThat(priced.total().amount()).isEqualTo(6000);
    }
}
