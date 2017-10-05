package de.kawachee.typo3_xliff.transfomers;

public class PLAIN_TEXT implements Transformer {

    private final Transformer internalTransformer;

    public PLAIN_TEXT() {
        this.internalTransformer = new HTML();
    }

    @Override
    public String transformValue(String input) {
        return internalTransformer.transformValue(input);
    }

    @Override
    public String replacement(boolean hasArguments) {
        return internalTransformer.replacement(hasArguments);
    }
}
