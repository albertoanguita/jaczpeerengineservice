package jacz.peerengineservice.util.datatransfer.test;

import jacz.util.numeric.range.LongRange;
import jacz.peerengineservice.util.datatransfer.master.ResourcePart;

/**
 *
 */
public class TestResourcePart {

    public static void main(String args[]) {

        ResourcePart resourcePart = new ResourcePart();

        add(resourcePart, 11, 12);
        add(resourcePart, 15, 18);
        add(resourcePart, 20, 21);
        add(resourcePart, 1, 2);
        add(resourcePart, 14, 14);

        System.out.println(resourcePart);

        //remove(resourcePart, 11, 12);

        for (int i = 0; i < 11; i++) {
            System.out.println(resourcePart.getPosition(i));
        }

        System.out.println(resourcePart);
        System.out.println("FIN");
    }

    private static void add(ResourcePart resourcePart, int min, int max) {
        resourcePart.add(new LongRange((long) min, (long) max));
    }

    private static void remove(ResourcePart resourcePart, int min, int max) {
        resourcePart.remove(new LongRange((long) min, (long) max));
    }
}
