package io.rappt.layout;

import org.xml.sax.SAXException;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Layout {

    public static final String SAMPLE_1 = "> t1 > t2 > t3 > t4 > t5 > b1";
    public static final String SAMPLE_2 = "> t1 | t2 > t3 > t4 > t5 > b1";
    public static final String SAMPLE_3 = "> t1 > t2 | t3 | t4 > t5 > b1";
    public static final String SAMPLE_4 = "> t1 > t2 | t3 | t4 > t5 | b1";
    public static final String SAMPLE_5 = "> t1 > t2 > t3 > t4 > t5 | t5 | t5| t5 | b1";
    public static final String SAMPLE_6 = "> t1 | t1 | t2 | t2 | t3 > t4 > t5 | t5 | t5| t5 | b1";
    public static final String SAMPLE_7 = "> t5 > t1 > b1 > t2 > t4 > t3";
    public static final String SAMPLE_8 = "> t5 > t1 > |b1 > t2 > t4 > t3";
    public static final String SAMPLE_9 = "> t5 > t1 > |b1 | t2 > t4 > t3";
    public static final String SAMPLE_10 = "> t5 | t1 > b1 > t2 > t4 > t3";
    public static final String SAMPLE_11 = "> t5 | t1 > | b1 | t2 > t4 > t3";
    public static final String SAMPLE_12 = "> t3 | t1 > t5 | t1 >  b1 > t2 > t4";
    public static final String SAMPLE_13 = "> b1 > t3 | t1 > t5 | t1 > t2 > t4";
    public static final String SAMPLE_14 = "> t3 > t1 | t5 > t1 |  b1 > t2 > t4";
    public static final String SAMPLE_15 = "> t2 > t4> b1 > t1 | t3 > t1 | t5 ";
    public static final String SAMPLE_16 = "> t1 | t3 > t1 | t5 > t1 |  b1 > t1 | t2 > t1 | t4";
    public static final String SAMPLE_17 = "> t2 > b1 > t1 | t3 | t4 > t1 | t5 ";
    public static final String SAMPLE_18 = "> t2 > b1 > t3 | t4 | t1  > t5 | t1";
    public static final Path INPUT_FILE = Paths.get("/Users/scottbarnett/projects/AndroidGen/MovieBase/app/src/main/res/layout/rowid_old.xml");
    public static final Path OUTPUT_FILE = Paths.get("/Users/scottbarnett/projects/AndroidGen/MovieBase/app/src/main/res/layout/rowid_new.xml");

    private static final Logger logger = Logger.getLogger(Layout.class.getName());

    Model model;

    public static void main(String... args) {
        try {
            Layout layout = new Layout(SAMPLE_4, INPUT_FILE);
            if (layout.parse(OUTPUT_FILE)) {
                layout.model.getErrors().forEach(logger::warning);
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.log(Level.SEVERE, "Error occurred " + e.toString(), e);
        }
    }

    public Layout( String layout,Path input) throws IOException, SAXException {
        this.model = new Model(layout, input);
    }

    public boolean parse( Path outputFile) throws IOException {
        boolean hasErrors = true;
        if (!model.applyRow()) {
            model.applyRowValues();
            model.addLinearAttributes();
            model.updateDocument();
            model.writeXmlFile(outputFile);
            hasErrors = false;
        }
        return hasErrors;
    }

    // TODO: Support overlapping multi lines?
    // TODO: Text cannot be multiline atm
}
