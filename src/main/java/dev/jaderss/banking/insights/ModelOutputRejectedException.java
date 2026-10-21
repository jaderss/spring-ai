package dev.jaderss.banking.insights;

public class ModelOutputRejectedException extends RuntimeException {

    public ModelOutputRejectedException(int attempts, String lastProblem) {
        super("the model gave no valid answer in " + attempts + " attempts, last problem: " + lastProblem);
    }
}
