package com.example.task03;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.function.Predicate;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Проверяет ту часть задания, которую предстоит реализовать: класс Hours, метод TimeUnit.getHours()
 * и конвертацию часов в TimeUnitUtils.
 * Класса Hours и метода getHours() пока не существует, поэтому они ищутся через рефлексию -
 * иначе остальные тесты модуля перестали бы компилироваться.
 */
public class HoursTest {

    private static final String HOURS_CLASS_NAME = "com.example.task03.Hours";

    private static final long MILLIS_IN_HOUR = 3_600_000L;

    private final Class<?> hoursClass;

    {
        try {
            hoursClass = Class.forName(HOURS_CLASS_NAME);
        } catch (final ClassNotFoundException e) {
            throw new AssertionError("Не найден ожидаемый класс интервала в часах " + HOURS_CLASS_NAME, e);
        }
    }

    @Test
    public void testHoursImplementsTimeUnit() {
        Assertions.assertTrue(TimeUnit.class.isAssignableFrom(hoursClass), "Класс Hours должен реализовывать интерфейс TimeUnit");
    }

    @Test
    public void testGetHoursDeclaredInTimeUnit() {
        Assertions.assertEquals(long.class, getGetHoursMethod().getReturnType(), "Метод getHours() должен возвращать long");
    }

    @Test
    public void testHours() {
        assertTimeUnit(newHours(1), MILLIS_IN_HOUR, 3600, 60, 1);
    }

    @Test
    public void testHours2() {
        assertTimeUnit(newHours(25), 25 * MILLIS_IN_HOUR, 90_000, 1500, 25);
    }

    @Test
    public void testMillisecondsToHours() {
        assertTimeUnit(new Milliseconds(1_800_000), 1_800_000, 1800, 30, 1);
    }

    @Test
    public void testMillisecondsToHours2() {
        assertTimeUnit(new Milliseconds(1_799_999), 1_799_999, 1800, 30, 0);
    }

    @Test
    public void testSecondsToHours() {
        assertTimeUnit(new Seconds(1800), 1_800_000, 1800, 30, 1);
    }

    @Test
    public void testSecondsToHours2() {
        assertTimeUnit(new Seconds(1799), 1_799_000, 1799, 30, 0);
    }

    @Test
    public void testMinutesToHours() {
        assertTimeUnit(new Minutes(30), 1_800_000, 1800, 30, 1);
    }

    @Test
    public void testMinutesToHours2() {
        assertTimeUnit(new Minutes(29), 1_740_000, 1740, 29, 0);
    }

    @Test
    public void testMinutesToHours3() {
        assertTimeUnit(new Minutes(90), 5_400_000, 5400, 90, 2);
    }

    @Test
    public void testUtilsToHours() {
        final Method conversion = findConversionToHours();
        final TimeUnit source = newTimeUnit(conversion.getParameterTypes()[0], 7_200_000L);
        final Object converted = invoke(conversion, null, source);
        Assertions.assertEquals(getHours(source), getHours((TimeUnit) converted), "Конвертация в часы не должна менять продолжительность интервала");
    }

    @Test
    public void testUtilsFromHours() {
        final Method conversion = findConversionFromHours();
        final TimeUnit converted = (TimeUnit) invoke(conversion, null, newHours(3));
        Assertions.assertEquals(3 * MILLIS_IN_HOUR, converted.toMillis(), "Конвертация из часов не должна менять продолжительность интервала");
    }

    /**
     * Проверяет ожидаемое значение интервала в различных единицах с реальными
     *
     * @param timeUnit временной интервал
     * @param expectedMillis ожидаемое количество миллисекунд
     * @param expectedSeconds ожидаемое количество секунд
     * @param expectedMinutes ожидаемое количество минут
     * @param expectedHours ожидаемое количество часов
     */
    private void assertTimeUnit(final TimeUnit timeUnit,
                                final long expectedMillis,
                                final long expectedSeconds,
                                final long expectedMinutes,
                                final long expectedHours) {
        Assertions.assertEquals(expectedMillis, timeUnit.toMillis(), "Количество миллисекунд не соответствует ожидаемому");
        Assertions.assertEquals(expectedSeconds, timeUnit.toSeconds(), "Количество секунд не соответствует ожидаемому");
        Assertions.assertEquals(expectedMinutes, timeUnit.toMinutes(), "Количество минут не соответствует ожидаемому");
        Assertions.assertEquals(expectedHours, getHours(timeUnit), "Количество часов не соответствует ожидаемому");
    }

    private Method getGetHoursMethod() {
        try {
            return TimeUnit.class.getMethod("getHours");
        } catch (final NoSuchMethodException e) {
            throw new AssertionError("В интерфейс TimeUnit нужно добавить метод long getHours()", e);
        }
    }

    private long getHours(final TimeUnit timeUnit) {
        return (long) invoke(getGetHoursMethod(), timeUnit);
    }

    private TimeUnit newHours(final long amount) {
        return newTimeUnit(hoursClass, amount);
    }

    private TimeUnit newTimeUnit(final Class<?> timeUnitClass, final long amount) {
        if (!TimeUnit.class.isAssignableFrom(timeUnitClass)) {
            throw new AssertionError(MessageFormat.format("Класс {0} должен реализовывать интерфейс TimeUnit", timeUnitClass.getName()));
        }
        try {
            return (TimeUnit) timeUnitClass.getConstructor(long.class).newInstance(amount);
        } catch (final NoSuchMethodException e) {
            throw new AssertionError(MessageFormat.format("У класса {0} должен быть публичный конструктор с параметром long", timeUnitClass.getName()), e);
        } catch (final InstantiationException | IllegalAccessException e) {
            throw new AssertionError(MessageFormat.format("Не удалось создать экземпляр класса {0}", timeUnitClass.getName()), e);
        } catch (final InvocationTargetException e) {
            throw new AssertionError(MessageFormat.format("Конструктор класса {0} завершился с ошибкой: {1}", timeUnitClass.getName(), e.getCause()), e.getCause());
        }
    }

    /**
     * Название методов конвертации в задании не зафиксировано, поэтому ищем их по сигнатуре
     *
     * @return метод, конвертирующий любой интервал в часы
     */
    private Method findConversionToHours() {
        return findConversion(m -> m.getReturnType() == hoursClass
                        && m.getParameterTypes()[0] != hoursClass
                        && TimeUnit.class.isAssignableFrom(m.getParameterTypes()[0]),
                "В TimeUnitUtils должен быть статический метод, конвертирующий интервал в часы");
    }

    /**
     * @return метод, конвертирующий часы в любой другой интервал
     */
    private Method findConversionFromHours() {
        return findConversion(m -> m.getParameterTypes()[0] == hoursClass
                        && m.getReturnType() != hoursClass
                        && TimeUnit.class.isAssignableFrom(m.getReturnType()),
                "В TimeUnitUtils должен быть статический метод, конвертирующий часы в другие единицы измерения");
    }

    private Method findConversion(final Predicate<Method> signature, final String notFoundMessage) {
        final Method[] methods = Arrays.stream(TimeUnitUtils.class.getDeclaredMethods())
                .filter(m -> Modifier.isStatic(m.getModifiers()))
                .filter(m -> Modifier.isPublic(m.getModifiers()))
                .filter(m -> m.getParameterCount() == 1)
                .filter(signature)
                .toArray(Method[]::new);
        if (methods.length == 0) {
            throw new AssertionError(notFoundMessage);
        }

        return methods[0];
    }

    private Object invoke(final Method method, final Object target, final Object... args) {
        try {
            return method.invoke(target, args);
        } catch (final IllegalAccessException e) {
            throw new AssertionError(MessageFormat.format("Метод {0} должен быть публичным", method.getName()), e);
        } catch (final InvocationTargetException e) {
            throw new AssertionError(MessageFormat.format("Метод {0} завершился с ошибкой: {1}", method.getName(), e.getCause()), e.getCause());
        }
    }

}
