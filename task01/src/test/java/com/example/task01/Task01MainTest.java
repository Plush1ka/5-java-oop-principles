package com.example.task01;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class Task01MainTest {

    private static final String LOGGER_CLASS_NAME = "com.example.task01.Logger";

    private static final Pattern MESSAGE_PATTERN =
            Pattern.compile("\\[([^]]+)] (\\d{4}\\.\\d{2}\\.\\d{2}) \\d{2}:\\d{2}:\\d{2} (.*) - (.*)");

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    private final Class<?> loggerClass;

    {
        try {
            loggerClass = Class.forName(LOGGER_CLASS_NAME);
        } catch (final ClassNotFoundException e) {
            throw new AssertionError("Не найден ожидаемый класс логгера " + LOGGER_CLASS_NAME, e);
        }
    }

    @Test
    public void testGetNameMethod() {
        final Method getName = getLoggerMethod("getName", 0);
        if (getName.getReturnType() != String.class) {
            throw new AssertionError("Метод getName() должен возвращать String");
        }
        Assertions.assertEquals("getName", invoke(getName, newLogger("getName")), "Метод getName() должен возвращать имя, с которым логгер был создан");
    }

    @Test
    public void testGetLoggerMethod() {
        getGetLoggerMethod();
    }

    @Test
    public void testGetLoggerReturnsSameInstance() {
        Assertions.assertSame(newLogger("sameInstance"),
                           newLogger("sameInstance"),
                           "Повторный вызов getLogger с тем же именем должен возвращать тот же экземпляр");
        Assertions.assertNotSame(newLogger("firstInstance"),
                              newLogger("secondInstance"),
                              "Вызов getLogger с разными именами должен возвращать разные экземпляры");
    }

    @Test
    public void testGetLevelMethod() {
        final Method getter = getLoggerMethod("getLevel", 0);
        if (getter.getReturnType() == void.class) {
            throw new AssertionError("Метод getLevel() должен возвращать значение");
        }
        if (getter.getReturnType() != getLevelType()) {
            throw new AssertionError("Метод getLevel() должен возвращать тот же тип, который принимает setLevel(...)");
        }
    }

    @Test
    public void testSetLevelMethod() {
        final Method setter = getLoggerMethod("setLevel", 1);
        if (!Modifier.isPublic(setter.getModifiers())) {
            throw new AssertionError("Метод setLevel должен быть публичным");
        }
        final Object logger = newLogger("setLevel");
        final Object level = getLevelValue("WARNING");
        invoke(setter, logger, level);
        Assertions.assertEquals(level,
                             invoke(getLoggerMethod("getLevel", 0), logger),
                             "Метод getLevel() должен возвращать уровень, установленный методом setLevel(...)");
    }

    @Test
    public void testDebugMethod() {
        assertLevelMethods("debug");
    }

    @Test
    public void testInfoMethod() {
        assertLevelMethods("info");
    }

    @Test
    public void testWarningMethod() {
        assertLevelMethods("warning");
    }

    @Test
    public void testErrorMethod() {
        assertLevelMethods("error");
    }

    @Test
    public void testLogMethods() {
        final Method log = getLoggerMethod("log", 2);
        if (log.getParameterTypes()[0] != getLevelType()) {
            throw new AssertionError("Первый аргумент метода log должен быть уровнем важности");
        }
        if (log.getParameterTypes()[1] != String.class) {
            throw new AssertionError("Второй аргумент метода log должен быть String");
        }

        final Method logTemplate = getLoggerMethod("log", 3);
        if (logTemplate.getParameterTypes()[0] != getLevelType()) {
            throw new AssertionError("Первый аргумент метода log должен быть уровнем важности");
        }
        if (logTemplate.getParameterTypes()[1] != String.class) {
            throw new AssertionError("Второй аргумент метода log должен быть String");
        }
        if (!logTemplate.isVarArgs() || logTemplate.getParameterTypes()[2] != Object[].class) {
            throw new AssertionError("Третий аргумент метода log должен быть Object...");
        }
    }

    @Test
    public void testLogMessage() {
        final Method error = getLoggerMethod("error", 1);
        final String dateBefore = LocalDate.now().format(DATE_FORMAT);
        final String output = capture(() -> invoke(error, newLogger("logMessage"), "test message"));
        final String dateAfter = LocalDate.now().format(DATE_FORMAT);

        final Matcher matcher = MESSAGE_PATTERN.matcher(output);
        if (!matcher.matches()) {
            throw new AssertionError(MessageFormat.format(
                    "Сообщение не соответcтвует ожидаемому шаблону: [ERROR] YYYY.MM.DD hh:mm:ss logMessage - test message, получено: {0}",
                    output));
        }
        Assertions.assertEquals("ERROR", matcher.group(1), "Уровень важности не соответствует ожидаемому");
        Assertions.assertTrue(dateBefore.equals(matcher.group(2)) || dateAfter.equals(matcher.group(2)),
                           "Дата в сообщении должна быть текущей, получено: " + matcher.group(2));
        Assertions.assertEquals("logMessage", matcher.group(3), "Имя логгера не соответствует ожидаемому");
        Assertions.assertEquals("test message", matcher.group(4), "Сообщение логгера не соответствует ожидаемому");
    }

    @Test
    public void testMessageTemplate() {
        final Method error = getLoggerMethod("error", 2);
        // Формат шаблона в задании не зафиксирован, поэтому засчитываем и String.format, и MessageFormat.format
        final String printfOutput = capture(() -> invoke(error, newLogger("printfTemplate"), "value %s", new Object[]{"42"}));
        final String messageFormatOutput = capture(() -> invoke(error, newLogger("messageFormatTemplate"), "value {0}", new Object[]{"42"}));

        Assertions.assertTrue(
            printfOutput.endsWith("value 42") || messageFormatOutput.endsWith("value 42"),
            MessageFormat.format(
                        "Аргументы должны подставляться в шаблон сообщения, получено: ''{0}'' и ''{1}''", printfOutput, messageFormatOutput));
    }

    @Test
    public void testLevelFiltering() {
        final Method setLevel = getLoggerMethod("setLevel", 1);
        final Method debug = getLoggerMethod("debug", 1);
        final Method error = getLoggerMethod("error", 1);
        final Object errorLevel = getLevelValue("ERROR");

        final String output = capture(() -> {
            final Object logger = newLogger("levelFiltering");
            invoke(setLevel, logger, errorLevel);
            invoke(debug, logger, "hidden message");
            invoke(error, logger, "visible message");
        });

        Assertions.assertFalse(output.contains("hidden message"),
                            "Сообщение с уровнем важности ниже установленного не должно печататься, получено: " + output);
        Assertions.assertTrue(output.contains("visible message"),
                           "Сообщение с уровнем важности не ниже установленного должно печататься, получено: " + output);
    }

    private void assertLevelMethods(final String levelMethodName) {
        final Method message = getLoggerMethod(levelMethodName, 1);
        if (message.getParameterTypes()[0] != String.class) {
            throw new AssertionError(MessageFormat.format("Аргумент метода {0} должен быть String", levelMethodName));
        }

        final Method template = getLoggerMethod(levelMethodName, 2);
        if (template.getParameterTypes()[0] != String.class) {
            throw new AssertionError(MessageFormat.format("Первый аргумент метода {0} должен быть String", levelMethodName));
        }
        if (!template.isVarArgs() || template.getParameterTypes()[1] != Object[].class) {
            throw new AssertionError(MessageFormat.format("Второй аргумент метода {0} должен быть Object...", levelMethodName));
        }
    }

    private Method getLoggerMethod(final String methodName, final int argsCount) {
        final Method[] methods = Arrays.stream(loggerClass.getDeclaredMethods())
                .filter(m -> m.getName().equals(methodName))
                .filter(m -> m.getParameterCount() == argsCount)
                .toArray(Method[]::new);
        if (methods.length == 0) {
            throw new AssertionError(MessageFormat.format("Не найден метод {0} c количеством параметров {1}", methodName, argsCount));
        }
        if (methods.length > 1) {
            throw new AssertionError(MessageFormat.format("Найдено несколько методов {0} c количеством параметров {1}", methodName, argsCount));
        }

        return methods[0];
    }

    private Method getGetLoggerMethod() {
        final Method getLogger = getLoggerMethod("getLogger", 1);
        final int modifiers = getLogger.getModifiers();
        if (!Modifier.isPublic(modifiers)) {
            throw new AssertionError("Метод getLogger должен быть публичным");
        }
        if (!Modifier.isStatic(modifiers)) {
            throw new AssertionError("Метод getLogger должен быть статическим");
        }
        if (getLogger.getParameterTypes()[0] != String.class) {
            throw new AssertionError("Аргумент метода getLogger должен быть String");
        }
        if (getLogger.getReturnType() != loggerClass) {
            throw new AssertionError("Метод getLogger должен возвращать Logger");
        }

        return getLogger;
    }

    private Object newLogger(final String name) {
        return invoke(getGetLoggerMethod(), null, name);
    }

    private Class<?> getLevelType() {
        return getLoggerMethod("setLevel", 1).getParameterTypes()[0];
    }

    /**
     * Ищет значение уровня важности по его имени - как константу перечисления, так и статическое поле
     *
     * @param levelName имя уровня важности
     * @return значение уровня важности
     */
    private Object getLevelValue(final String levelName) {
        final Class<?> levelType = getLevelType();
        if (levelType.isEnum()) {
            for (final Object constant : levelType.getEnumConstants()) {
                if (((Enum<?>) constant).name().equalsIgnoreCase(levelName)) {
                    return constant;
                }
            }
        }
        for (final Class<?> holder : new Class<?>[]{levelType, loggerClass}) {
            for (final Field field : holder.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers())
                        || !field.getName().equalsIgnoreCase(levelName)
                        || !levelType.isAssignableFrom(field.getType())) {
                    continue;
                }
                field.setAccessible(true);
                try {
                    return field.get(null);
                } catch (final IllegalAccessException e) {
                    throw new AssertionError(MessageFormat.format("Не удалось прочитать уровень важности {0}", levelName), e);
                }
            }
        }

        throw new AssertionError(MessageFormat.format(
                "Не найден уровень важности {0} среди значений типа {1}", levelName, levelType.getName()));
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

    /**
     * Перехватывает вывод логгера в консоль на время вызова
     *
     * @param invocation вызов логгера
     * @return всё, что было выведено в System.out и System.err
     */
    private String capture(final Runnable invocation) {
        final PrintStream systemOut = System.out;
        final PrintStream systemErr = System.err;
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try {
            final PrintStream testStream = new PrintStream(buffer, true, StandardCharsets.UTF_8.name());
            System.setOut(testStream);
            System.setErr(testStream);
            invocation.run();
            testStream.flush();
        } catch (final UnsupportedEncodingException e) {
            throw new AssertionError(e);
        } finally {
            System.setOut(systemOut);
            System.setErr(systemErr);
        }

        return new String(buffer.toByteArray(), StandardCharsets.UTF_8).trim();
    }

}
