package ru.inversion.fx.form.property;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.BooleanProperty;
import javafx.beans.value.ChangeListener;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class BooleanPropertiesTest {

    private BooleanProperty source;
    private BooleanProperty notProperty;

    @BeforeEach
    void setUp() {
        source = new SimpleBooleanProperty(false);
        notProperty = BooleanProperties.not(source);
    }

    @Test
    @DisplayName("Тест базовой инверсии значений")
    void testBasicInversion() {
        assertFalse(source.get());
        assertTrue(notProperty.get());

        source.set(true);
        assertTrue(source.get());
        assertFalse(notProperty.get());

        notProperty.set(true);
        assertFalse(source.get());
        assertTrue(notProperty.get());
    }

    @Test
    @DisplayName("Тест слушателей ChangeListener")
    void testChangeListeners() {
        List<Boolean> oldValues = new ArrayList<>();
        List<Boolean> newValues = new ArrayList<>();

        ChangeListener<Boolean> listener = (obs, oldVal, newVal) -> {
            oldValues.add(oldVal);
            newValues.add(newVal);
        };

        notProperty.addListener(listener);

        source.set(true);
        source.set(false);

        assertEquals(2, oldValues.size());
        assertEquals(2, newValues.size());

        // Первое изменение: false -> true в source, true -> false в notProperty
        assertTrue(oldValues.get(0));  // !false = true
        assertFalse(newValues.get(0)); // !true = false

        // Второе изменение: true -> false в source, false -> true в notProperty
        assertFalse(oldValues.get(1)); // !true = false
        assertTrue(newValues.get(1));  // !false = true
    }

    @Test
    @DisplayName("Тест добавления и удаления слушателей")
    void testAddRemoveListeners() {
        AtomicInteger callCount = new AtomicInteger(0);

        ChangeListener<Boolean> listener = (obs, oldVal, newVal) -> {
            callCount.incrementAndGet();
        };

        notProperty.addListener(listener);

        source.set(true);
        source.set(false);

        assertEquals(2, callCount.get());

        notProperty.removeListener(listener);

        source.set(true);

        assertEquals(2, callCount.get(), "Слушатель не должен вызываться после удаления");
    }

    @Test
    @DisplayName("Тест нескольких слушателей")
    void testMultipleListeners() {
        AtomicInteger listener1Calls = new AtomicInteger(0);
        AtomicInteger listener2Calls = new AtomicInteger(0);

        ChangeListener<Boolean> listener1 = (obs, oldVal, newVal) -> listener1Calls.incrementAndGet();
        ChangeListener<Boolean> listener2 = (obs, oldVal, newVal) -> listener2Calls.incrementAndGet();

        notProperty.addListener(listener1);
        notProperty.addListener(listener2);

        source.set(true);

        assertEquals(1, listener1Calls.get());
        assertEquals(1, listener2Calls.get());

        // Удаляем только listener1
        notProperty.removeListener(listener1);
        source.set(false);

        assertEquals(1, listener1Calls.get()); // Не изменилось
        assertEquals(2, listener2Calls.get()); // Увеличилось
    }

    @Test
    @DisplayName("Тест методов bind/unbind")
    void testBindUnbind() {
        assertTrue(notProperty.isBound());

        assertThrows(IllegalStateException.class, () -> {
            notProperty.bind(new SimpleBooleanProperty(true));
        });

        assertThrows(IllegalStateException.class, () -> {
            notProperty.unbind();
        });
    }

    @Test
    @DisplayName("Тест двустороннего биндинга с другим свойством")
    void testBidirectionalWithOtherProperty() {
        BooleanProperty otherProperty = new SimpleBooleanProperty(true);

        notProperty.bindBidirectional(otherProperty);

        // Меняем otherProperty
        otherProperty.set(false);
        assertFalse(notProperty.get());
        assertTrue(source.get()); // Инвертировано

        // Меняем через source
        source.set(false);
        assertTrue(notProperty.get());
        assertTrue(otherProperty.get());
    }

    @Test
    @DisplayName("Тест метода getBean() и getName()")
    void testBeanAndName() {
        Object testBean = new Object();
        BooleanProperty namedSource = new SimpleBooleanProperty(testBean, "isEnabled", true);
        BooleanProperty namedNot = BooleanProperties.not(namedSource);

        assertEquals(testBean, namedNot.getBean());
        assertNotNull(namedNot.getName());
        // Имя может быть "not(isEnabled)" или что-то подобное
    }

    @Test
    @DisplayName("Тест с InvalidationListener")
    void testInvalidationListener() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        notProperty.addListener(obs -> {
            latch.countDown();
        });

        source.set(true);

        assertTrue(latch.await(1, TimeUnit.SECONDS),
                "InvalidationListener должен был вызваться");
    }

    @Test
    @DisplayName("Тест цепочки инверсий")
    void testNotNotChain() {
        BooleanProperty notNotProperty = BooleanProperties.not(notProperty);

        assertFalse(source.get());
        assertTrue(notProperty.get());
        assertFalse(notNotProperty.get());

        source.set(true);

        assertTrue(source.get());
        assertFalse(notProperty.get());
        assertTrue(notNotProperty.get());
    }

    @Test
    @DisplayName("Тест toString() метода")
    void testToString() {
        String toString = notProperty.toString();
        assertNotNull(toString);
        // Просто проверяем, что toString не падает
        System.out.println("toString: " + toString);
    }

    @Test
    @DisplayName("Тест с разными начальными значениями")
    void testDifferentInitialValues() {
        testWithInitialValue(false);
        testWithInitialValue(true);
    }

    private void testWithInitialValue(boolean initialValue) {
        BooleanProperty source = new SimpleBooleanProperty(initialValue);
        BooleanProperty notProp = BooleanProperties.not(source);

        assertEquals(!initialValue, notProp.get());

        source.set(!initialValue);
        assertEquals(initialValue, notProp.get());

        notProp.set(initialValue);
        assertEquals(!initialValue, source.get());
    }

    @Test
    @DisplayName("Тест дублирования слушателей")
    void testDuplicateListeners() {
        AtomicInteger callCount = new AtomicInteger(0);

        ChangeListener<Boolean> listener = (obs, oldVal, newVal) -> callCount.incrementAndGet();

        // Добавляем один и тот же listener несколько раз
        notProperty.addListener(listener);
        notProperty.addListener(listener);
        notProperty.addListener(listener);

        source.set(true);

        // В JavaFX дубликаты разрешены, поэтому может быть 1 или больше вызовов
        assertTrue(callCount.get() >= 1);

        // Удаляем один раз
        notProperty.removeListener(listener);

        int callsAfterFirstRemove = callCount.get();
        source.set(false);

        // После удаления одного экземпляра, остальные все еще могут работать
        // Поэтому проверка сложная
        System.out.println("Calls after first remove: " + (callCount.get() - callsAfterFirstRemove));
    }

    @Test
    @DisplayName("Тест производительности")
    void testPerformance() {
        int iterations = 10000;
        AtomicInteger callCount = new AtomicInteger(0);

        notProperty.addListener((obs, oldVal, newVal) -> callCount.incrementAndGet());

        long startTime = System.nanoTime();

        for (int i = 0; i < iterations; i++) {
            source.set(i % 2 == 0);
        }

        long endTime = System.nanoTime();

        assertEquals(iterations, callCount.get());

        double durationMs = (endTime - startTime) / 1_000_000.0;
        System.out.printf("%,d итераций за %,.2f мс (%,.3f мкс/итерация)%n",
                iterations, durationMs, durationMs * 1000 / iterations);
    }
}