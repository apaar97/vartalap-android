package com.sih.android.accenttranslatorvoip.feature;

public class ADPCM {

    private static final byte[] stepIndexTable = {
            8, 6, 4, 2, -1, -1, -1, -1, -1, -1, -1, -1, 2, 4, 6, 8
    };

    private static final short[] stepTable = {
            7, 8, 9, 10, 11, 12, 13, 14, 16, 17,
            19, 21, 23, 25, 28, 31, 34, 37, 41, 45,
            50, 55, 60, 66, 73, 80, 88, 97, 107, 118,
            130, 143, 157, 173, 190, 209, 230, 253, 279, 307,
            337, 371, 408, 449, 494, 544, 598, 658, 724, 796,
            876, 963, 1060, 1166, 1282, 1411, 1552, 1707, 1878, 2066,
            2272, 2499, 2749, 3024, 3327, 3660, 4026, 4428, 4871, 5358,
            5894, 6484, 7132, 7845, 8630, 9493, 10442, 11487, 12635, 13899,
            15289, 16818, 18500, 20350, 22385, 24623, 27086, 29794, 32767
    };

    private int leftStepIndexEnc, rightStepIndexEnc, leftPredictedEnc, rightPredictedEnc;
    private int leftStepIndexDec, rightStepIndexDec, leftPredictedDec, rightPredictedDec;

    /**
     * Creates a ADPCM encoder/decoder.
     */
    public ADPCM() {
        this.resetEncoder();
        this.resetDecoder();
    }

    /**
     * Reset the ADPCM predictor.
     * Call when encoding a new stream.
     */
    public void resetEncoder() {
        leftStepIndexEnc = rightStepIndexEnc = 0;
        leftPredictedEnc = rightPredictedEnc = 0;
    }

    /**
     * Reset the ADPCM predictor.
     * Call when decoding a new stream.
     */
    public void resetDecoder() {
        leftStepIndexDec = rightStepIndexDec = 0;
        leftPredictedDec = rightPredictedDec = 0;
    }

    /**
     * Encode 16-bit stereo little-endian PCM audio data to 8-bit ADPCM audio data.
     *
     * @param input 16-bit stereo little-endian PCM audio data
     * @return 8-bit ADPCM audio data
     */
    public byte[] encode(short[] input) {
        int count = input.length / 2;
        byte[] output = new byte[count];

        int inputIndex = 0, outputIndex = 0;
        while (outputIndex < count) {
            int leftSample = input[inputIndex++];
            int rightSample = input[inputIndex++];
            int leftStep = stepTable[leftStepIndexEnc];
            int rightStep = stepTable[rightStepIndexEnc];
            int leftCode = ((leftSample - leftPredictedEnc) * 4 + leftStep * 8) / leftStep;
            int rightCode = ((rightSample - rightPredictedEnc) * 4 + rightStep * 8) / rightStep;
            if (leftCode > 15) leftCode = 15;
            if (rightCode > 15) rightCode = 15;
            if (leftCode < 0) leftCode = 0;
            if (rightCode < 0) rightCode = 0;
            leftPredictedEnc += ((leftCode * leftStep) >>> 2) - ((15 * leftStep) >>> 3);
            rightPredictedEnc += ((rightCode * rightStep) >>> 2) - ((15 * rightStep) >>> 3);
            if (leftPredictedEnc > 32767) leftPredictedEnc = 32767;
            if (rightPredictedEnc > 32767) rightPredictedEnc = 32767;
            if (leftPredictedEnc < -32768) leftPredictedEnc = -32768;
            if (rightPredictedEnc < -32768) rightPredictedEnc = -32768;
            leftStepIndexEnc += stepIndexTable[leftCode];
            rightStepIndexEnc += stepIndexTable[rightCode];
            if (leftStepIndexEnc > 88) leftStepIndexEnc = 88;
            if (rightStepIndexEnc > 88) rightStepIndexEnc = 88;
            if (leftStepIndexEnc < 0) leftStepIndexEnc = 0;
            if (rightStepIndexEnc < 0) rightStepIndexEnc = 0;

            //output[outputIndex++] = (byte) ((leftCode << 4) | rightCode);
            output[outputIndex++] = (byte) ((rightCode << 4) | rightCode);
        }
        return output;
    }

    /**
     * Decode 8-bit ADPCM audio data to 16-bit stereo little-endian PCM audio data.
     *
     * @param input 8-bit ADPCM audio data
     * @return 16-bit stereo little-endian PCM audio data
     */
    public short[] decode(byte[] input) {
        int count = input.length * 2;
        short[] output = new short[count];

        int inputIndex = 0, outputIndex = 0;
        while (outputIndex < count) {
            int leftCode = input[inputIndex++] & 0xFF;
            int rightCode = leftCode & 0xF;
            leftCode = leftCode >>> 4;
            int leftStep = stepTable[leftStepIndexDec];
            int rightStep = stepTable[rightStepIndexDec];
            leftPredictedDec += ((leftCode * leftStep) >>> 2) - ((15 * leftStep) >>> 3);
            rightPredictedDec += ((rightCode * rightStep) >>> 2) - ((15 * rightStep) >>> 3);
            if (leftPredictedDec > 32767) leftPredictedDec = 32767;
            if (rightPredictedDec > 32767) rightPredictedDec = 32767;
            if (leftPredictedDec < -32768) leftPredictedDec = -32768;
            if (rightPredictedDec < -32768) rightPredictedDec = -32768;
            output[outputIndex++] = (short) leftPredictedDec;
            output[outputIndex++] = (short) rightPredictedDec;
            leftStepIndexDec += stepIndexTable[leftCode];
            rightStepIndexDec += stepIndexTable[rightCode];
            if (leftStepIndexDec > 88) leftStepIndexDec = 88;
            if (rightStepIndexDec > 88) rightStepIndexDec = 88;
            if (leftStepIndexDec < 0) leftStepIndexDec = 0;
            if (rightStepIndexDec < 0) rightStepIndexDec = 0;
        }
        return output;
    }
}