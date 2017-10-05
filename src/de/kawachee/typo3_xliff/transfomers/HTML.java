package de.kawachee.typo3_xliff.transfomers;

public class HTML implements Transformer {

    @Override
    public String transformValue(String input) {
        return input;
    }

    @Override
    public String replacement(boolean hasArguments) {
        if (hasArguments) {
            return "<f:translate key=\"%s\" arguments=\"{}\" />";
        }

        return "<f:translate key=\"%s\" />";
    }
}
