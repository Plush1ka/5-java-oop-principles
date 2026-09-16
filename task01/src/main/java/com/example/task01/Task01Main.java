package com.example.task01;

public class Task01Main {
    public static void main(String[] args) {

        Logger logger = Logger.getLogger("myLogger");

        logger.setLevel(Logger.Level.WARNING);

        logger.debug("Start");
        logger.info("User online");
        logger.warning("Wrong password");
        logger.error("Server fall out");

        logger.info("Пользователь %s выполнил %d попыток входа", "user191", 9);
        logger.warning("Сервер загружен на %s%%", 93);

        logger.log(Logger.Level.INFO, "Нет данных.");
    }
}
