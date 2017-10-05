package de.kawachee.typo3_xliff.transfomers;

public interface Transformer {

    public String transformValue(String input);

    public String replacement(boolean hasArguments);

}
