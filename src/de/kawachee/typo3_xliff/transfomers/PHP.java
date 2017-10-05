package de.kawachee.typo3_xliff.transfomers;

public class PHP implements Transformer {

    @Override
    public String transformValue(String input) {
        StringBuilder temp = new StringBuilder(input);

        char firstChar = temp.charAt(0);
        if (firstChar == '"' || firstChar == '\'') {
            temp.deleteCharAt(0);
        }

        int end = temp.length() - 1;
        char lastChar = temp.charAt(end);
        if (lastChar == '"' || lastChar == '\'') {
            temp.deleteCharAt(end);
        }

        return temp.toString();
    }

    @Override
    public String replacement(boolean hasArguments) {
        if (hasArguments) {
            return "LocalizationUtility::translate(\"%s\", $this->extensionName, []);";
        }

        return "LocalizationUtility::translate(\"%s\", $this->extensionName);";
    }
}
