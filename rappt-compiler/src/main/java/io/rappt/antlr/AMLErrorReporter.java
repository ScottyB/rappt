package io.rappt.antlr;

import org.antlr.v4.runtime.Token;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

// Stores errors to display to the user
public interface AMLErrorReporter {

    List<String> errorList = new ArrayList<>();
    final Logger logger = Logger.getLogger(AMLErrorReporter.class.getName());

    public default void addError(Token token, String message) {
        addError(token.getLine(), token.getCharPositionInLine(), message);
    }

    public default void addError(int line, int charPos, String message) {
        errorList.add(errorFormat(line, charPos, message));
        logger.info(errorFormat(line, charPos, "UserError: " + message));
    }

    public static String errorFormat(int line, int charPos, String message) {
        return "line " + line + ":" + charPos + " " + message;
    }

    public default boolean hasErrors(){
        return !errorList.isEmpty();
    }

    public default List<String> getErrors() {
        return errorList;
    }

    public default void clearErrors() {
        errorList.clear();
    }

    default void addError(String error) {
        errorList.add(error);
    }
}
