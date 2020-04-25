package com.visualizingbackprop.test;

import java.util.ArrayList;
import java.util.Random;
import java.util.function.Function;

interface INetConstants {
    double learningRate = 0.5;
}

class Utility {
    double[][] multiplyMatrices(double[][] firstMatrix, double[][] secondMatrix) {
        double[][] result = new double[firstMatrix.length][secondMatrix[0].length];

        for (int row = 0; row < result.length; row++) {
            for (int col = 0; col < result[row].length; col++) {
                result[row][col] = multiplyMatricesCell(firstMatrix, secondMatrix, row, col);
            }
        }

        return result;
    }

    double multiplyMatricesCell(double[][] firstMatrix, double[][] secondMatrix, int row, int col) {
        double cell = 0;
        for (int i = 0; i < secondMatrix.length; i++) {
            cell += firstMatrix[row][i] * secondMatrix[i][col];
        }
        return cell;
    }

    double[][] map(double[][] input, Function<Double, Double> func) {
        double[][] result = new double[input.length][input[0].length];
        for(int i = 0; i < input.length; i++) {
            for (int j = 0; j < input[0].length; j++) {
                result[i][j] = func.apply(input[i][j]);
            }
        }
        return result;
    }

    double loss(double output, double exp) {
        return Math.pow(output - exp, 2);
    }
}

class Net {
    double[][] inputWeights;
    double[][] hiddenWeights;
    ArrayList<double[][]> inputs;
    ArrayList<double[][]> expectedOut;
    boolean activation;

    Net(ArrayList<double[][]> inputs, ArrayList<double[][]> expectedOut, boolean activ) {
        this.inputs = inputs;
        this.expectedOut = expectedOut;
        this.activation = activ;

        this.inputWeights = new double[2][2];
        this.hiddenWeights = new double[2][2];

        for (int i = 0; i < inputWeights.length; i++) {
            for(int j = 0; j < inputWeights[0].length; j++) {
                inputWeights[i][j] = new Random().nextFloat();
            }
        }
        for (int i = 0; i < hiddenWeights.length; i++) {
            for(int j = 0; j < hiddenWeights[0].length; j++) {
                hiddenWeights[i][j] = new Random().nextFloat();
            }
        }
    }

    public double sigmoid(double x) {
        return (1/( 1 + Math.pow(Math.E,(-1*x))));
    }

    public double rectLinear(double x) {
        if (x <= 0) {
            return 0;
        } else {
            return x;
        }
    }

    public double sigmoidDeriv(double x) {
        return this.sigmoid(x) * (1 - this.sigmoid(x));
    }

    public double rectLinearDeriv(double x) {
        if (x <= 0) {
            return 0;
        }
        else {
            return 1;
        }
    }

    public double[][] onlyFrontProp(double[][] input) {
        if (this.activation) {
            return this.onlyFrontPropFunc(input, (x -> this.sigmoid(x)));
        } else {
            return this.onlyFrontPropFunc(input, (x -> this.rectLinear(x)));
        }
    }

    public double[][] onlyFrontPropFunc(double[][] input, Function<Double, Double> activation) {
        Utility utils = new Utility();

        double[][] inA = utils.multiplyMatrices(this.inputWeights, input);
        double[][] outA = utils.map(inA, activation);

        double[][] inB = utils.multiplyMatrices(this.hiddenWeights, outA);
        double[][] outB = utils.map(inB, activation);

        return outB;
    }

    public double stochasticBackPropagation() {
        int randIndex = new Random().nextInt(this.inputs.size());

        if (this.activation) {
            return this.stochasticBackPropFunc(inputs.get(randIndex), expectedOut.get(randIndex),
                        (x -> this.sigmoid(x)), (x -> this.sigmoidDeriv(x)));
        } else {
            return this.stochasticBackPropFunc(inputs.get(randIndex), expectedOut.get(randIndex),
                        (x -> this.rectLinear(x)), (x -> this.rectLinearDeriv(x)));
        }
    }

    public double stochasticBackPropFunc(double[][] inputs, double[][] output, Function<Double, Double> activation,
                                         Function<Double, Double> activationDer) {
        Utility utils = new Utility();

        double[][] inA = utils.multiplyMatrices(this.inputWeights, inputs);
        double[][] outA = utils.map(inA, activation);

        double[][] inB = utils.multiplyMatrices(this.hiddenWeights, outA);
        double[][] outB = utils.map(inB, activation);

        double totalLossOne = utils.loss(outB[0][0], output[0][0]);
        double totalLossTwo = utils.loss(outB[1][0], output[1][0]);

        double deltaOut00 = 2 * (outB[0][0] - output[0][0]) * activationDer.apply(inB[0][0]) * outA[0][0];
        double deltaOut01 = 2 * (outB[0][0] - output[0][0]) * activationDer.apply(inB[1][0]) * outA[0][0];
        double deltaOut10 = 2 * (outB[1][0] - output[1][0]) * activationDer.apply(inB[0][0]) * outA[1][0];
        double deltaOut11 = 2 * (outB[1][0] - output[1][0]) * activationDer.apply(inB[1][0]) * outA[1][0];

        double deltaIn00 = (deltaOut00 + deltaOut10) * activationDer.apply(inA[0][0]) * inputs[0][0];
        double deltaIn01 = (deltaOut00 + deltaOut10) * activationDer.apply(inA[0][0]) * inputs[1][0];
        double deltaIn10 = (deltaOut01 + deltaOut11) * activationDer.apply(inA[1][0]) * inputs[0][0];
        double deltaIn11 = (deltaOut01 + deltaOut11) * activationDer.apply(inA[1][0]) * inputs[1][0];

        this.inputWeights[0][0] -= INetConstants.learningRate * deltaIn00;
        this.inputWeights[0][1] -= INetConstants.learningRate * deltaIn01;
        this.inputWeights[1][0] -= INetConstants.learningRate * deltaIn10;
        this.inputWeights[1][1] -= INetConstants.learningRate * deltaIn11;

        this.hiddenWeights[0][0] -= INetConstants.learningRate * deltaOut00;
        this.hiddenWeights[0][1] -= INetConstants.learningRate * deltaOut01;
        this.hiddenWeights[1][0] -= INetConstants.learningRate * deltaOut10;
        this.hiddenWeights[1][1] -= INetConstants.learningRate * deltaOut11;

        return totalLossOne + totalLossTwo;
    }

    double computeAccuracy() {
        double total = 0, counter = 0;

        for (int i = 0; i < this.inputs.size(); i++) {
            double[][] input = this.inputs.get(i);
            double[][] expOut = this.expectedOut.get(i);
            double[][] calcOut = this.onlyFrontProp(input);

            System.out.println("Output: " + calcOut[0][0] + " " + calcOut[1][0]);
            System.out.println("Expected: " + expOut[0][0] + " " + expOut[1][0]);
            System.out.println();

            if (Math.abs(calcOut[0][0] - expOut[0][0]) < 0.5 && Math.abs(calcOut[1][0] - expOut[1][0]) < 0.5) {
                counter++;
            }
            total++;
        }

        return counter / total;
    }
}

