package io.rappt.antlr;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import io.rappt.AMLParser;

public class AMLParseModel extends BaseErrorListener implements AMLErrorReporter {

    public AMLParser.ParseContext tree;

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                            int line, int charPositionInLine,
                            String msg, RecognitionException e) {
        this.addError(line, charPositionInLine, msg);
    }
}