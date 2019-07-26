package corda.samples.upgrades.contracts;

import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.security.MessageDigest;

public class Test {

    public static void main(String[] args) {
        int buff = 16384;
        try {
            RandomAccessFile file = new RandomAccessFile("/Users/ashutoshmeher/Corda/mysamples/cordapp-upgrades/contracts/v1-contracts/build/libs/v1-contracts.jar", "r");

            long startTime = System.nanoTime();
            MessageDigest hashSum = MessageDigest.getInstance("SHA-256");

            byte[] buffer = new byte[buff];
            byte[] partialHash = null;

            long read = 0;

            // calculate the hash of the hole file for the test
            long offset = file.length();
            int unitsize;
            while (read < offset) {
                unitsize = (int) (((offset - read) >= buff) ? buff : (offset - read));
                file.read(buffer, 0, unitsize);

                hashSum.update(buffer, 0, unitsize);

                read += unitsize;
            }

            file.close();
            partialHash = new byte[hashSum.getDigestLength()];
            partialHash = hashSum.digest();

            BigInteger no = new BigInteger(1, partialHash);
            String hashtext = no.toString(16);
            while (hashtext.length() < 32) {
                hashtext = "0" + hashtext;
            }
            //System.out.println(hashtext);

            System.out.println(hashtext.toUpperCase());

            long endTime = System.nanoTime();

            System.out.println(endTime - startTime);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
