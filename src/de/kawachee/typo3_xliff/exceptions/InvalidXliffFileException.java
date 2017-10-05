package de.kawachee.typo3_xliff.exceptions;

public class InvalidXliffFileException extends Exception {

    public InvalidXliffFileException(String missingTag) {
        super("Invalid XLIFF file! Missing \"" + missingTag + "\" tag.");
    }

}
