package ZBRA;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.time.StopWatch;

import ZBRA.tfm.AbstractTFM;
import ZBRA.tfm.Burning2ndPrice;
import ZBRA.tfm.EIP1559;
import ZBRA.tfm.FirstPrice;
import ZBRA.tfm.Pool;
import ZBRA.tfm.SecondPrice;

public class Main {
    public static void main(String[] args) throws IOException {
        // if (args.length < 6) {
        //     System.err.println("Error: Insufficient arguments provided. Usage: 
        //     java -jar simulator.jar <TFM_STYLE> <NUMBER_OF_BLOCK_CYCLES> <SEED> <NUMBER_OF_MINERS> <OUTPUT_FILENAME> <INPUT_FILENAME>");
        //     return;
        // }

        args = new String[]{"full", "4320", "7653", "15", "first-price-month-x", "transactions.json"};

        // if (tfm == null) {
        //     System.err.println("Error: Invalid TFM style provided. Please use one of the supported styles: first_price, second_price, eip1559, pool, burning_second_price.");
        //     return;
        // }

        int numberOfMiners = Integer.parseInt(args[3]);
        if (numberOfMiners <= 0) {
            System.err.println("Error: NUMBER_OF_MINERS must be greater than zero.");
            return;
        }
        String[] tfms = {"first_price", "second_price", "burning_second_price", "eip1559", "pool"};
        
        if (args[0].equals("full")) {
            Random rand = new Random();
            for (int i = 0; i < 20; i++) {
                for (String tfmName : tfms) {
                AbstractTFM tfm = switch (tfmName) {
                    case "first_price" -> new FirstPrice();
                    case "second_price" -> new SecondPrice();
                    case "burning_second_price" -> new Burning2ndPrice();
                    case "eip1559" -> new EIP1559();
                    case "pool" -> new Pool();
                    default -> null;
                };

                if (tfm != null) {
                    StopWatch sw = new StopWatch();
                    Simulation s = new Simulation(tfm,                          // tfm type
                                            Integer.parseInt(args[1]),          // number of block cycles
                                            rand.nextInt(),                     // seed
                                            Integer.parseInt(args[3]),          // number of miners
                                            (tfmName + "-month-" + args[2]),    // output filename
                                            args[5]);                           // input filename

                    sw.start();
                    s.simulate();
                    sw.stop();
                    System.out.println("\n[" + tfmName + "] timer: " + sw.getTime(TimeUnit.MILLISECONDS) + " ms");
                }
            }
            }
            
        }
        else {
            // single mode
            AbstractTFM tfm = switch (args[0]) {
                case "first_price" -> new FirstPrice();
                case "second_price" -> new SecondPrice();
                case "eip1559" -> new EIP1559();
                case "pool" -> new Pool();
                case "burning_second_price" -> new Burning2ndPrice();
                default -> null;
            };

            if (tfm != null) {
                StopWatch sw = new StopWatch();
                Simulation s = new Simulation(tfm, Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]), args[4], args[5]);

                sw.start();
                s.simulate();
                sw.stop();
                System.out.println("\ntimer: " + sw.getTime(TimeUnit.MILLISECONDS) + " ms");
            }
        }
    }
}