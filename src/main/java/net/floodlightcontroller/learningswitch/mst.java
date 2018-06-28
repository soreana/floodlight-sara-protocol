import java.util.InputMismatchException;
import java.util.Scanner;

public class mst{
    private boolean unsettled[];
    private boolean settled[];
    private int numOfSw;
    private int flowMatrix[][];
    private int key[];
    public static final int INFINITE = 999;
    private int parent[];

    public mst(int numOfSw){
        this.numOfSw = numOfSw;
        unsettled = new boolean[numOfSw + 1];
        settled = new boolean[numOfSw + 1];
        flowMatrix = new int[numOfSw + 1][numOfSw + 1];
        key = new int[numOfSw + 1];
        parent = new int[numOfSw + 1];
    }

    public int getUnsettledCount(boolean unsettled[]){
        int count = 0;
        for (int index = 0; index < unsettled.length; index++)
            if (unsettled[index])
                count++;
        return count;
    }

    public void primsAlgorithm(int flowMatrix[][]){
        int evaluationVertex;
        for (int src = 1; src <= numOfSw; src++)
            for (int dst = 1; dst <= numOfSw; dst++)
                this.flowMatrix[src][dst] = flowMatrix[src][dst];

        for (int index = 1; index <= numOfSw; index++)
            key[index] = INFINITE;
        key[1] = 0;
        unsettled[1] = true;
        parent[1] = 1;

        while (getUnsettledCount(unsettled) != 0){
            evaluationVertex = findMinWay(unsettled);
            unsettled[evaluationVertex] = false;
            settled[evaluationVertex] = true;
            evaluateNeighbours(evaluationVertex);
        }

    }

    private int findMinWay(boolean[] unsettled2){
        int min = Integer.MAX_VALUE;
        int node = 0;
        for (int vertex = 1; vertex <= numOfSw; vertex++) {
            if (unsettled[vertex] == true && key[vertex] < min) {
                node = vertex;
                min = key[vertex];
            }
        }
        return node;
    }

    public void evaluateNeighbours(int evaluationVertex) {
        for (int dstvertex = 1; dstvertex <= numOfSw; dstvertex++) {
            if (settled[dstvertex] == false) {
                if (flowMatrix[evaluationVertex][dstvertex] != INFINITE) {
                    if (flowMatrix[evaluationVertex][dstvertex] < key[dstvertex]) {
                        key[dstvertex] = flowMatrix[evaluationVertex][dstvertex];
                        parent[dstvertex] = evaluationVertex;
                    }
                    unsettled[dstvertex] = true;
                }
            }
        }
    }

    public static void main(String... arg) {
        int adjacency_matrix[][];
        int numOfSwitches;
        Scanner scan = new Scanner(System.in);
        try {
            System.out.println("Enter the number of switches");
            numOfSwitches = scan.nextInt();
            adjacency_matrix = new int[numOfSwitches + 1][numOfSwitches + 1];

            System.out.println("Enter the flow between switches");

            for (int i = 1; i <= numOfSwitches; i++) {
                for (int j = 1; j <= numOfSwitches; j++) {
                    adjacency_matrix[i][j] = scan.nextInt();
                    if (i == j) {
                        adjacency_matrix[i][j] = 0;
                        continue;
                    }
                    if (adjacency_matrix[i][j] == 0)
                        adjacency_matrix[i][j] = INFINITE;
                }

            }

            mst prims = new mst(numOfSwitches);
            prims.primsAlgorithm(adjacency_matrix);
            prims.printMST();

        } catch (InputMismatchException inputMismatch) {
            System.out.println("Wrong Input Format");
        }
        scan.close();
    }

    public void printMST() {
        System.out.println("\nMST\n");
        System.out.println("SOURCE  : DESTINATION = WEIGHT");
        for (int vertex = 2; vertex <= numOfSw; vertex++)
            System.out.println("S" + parent[vertex] + "\t:\t" + "S" + vertex +"\t=\t"+ flowMatrix[parent[vertex]][vertex]);
    }
}
