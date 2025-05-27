import java.io.*;
import java.util.*;

public class FlightPlanner {

    // A flight leg where you can go from a city, how much it costs, and how long it takes
    private static class FlightEdge {
        String dest;
        int cost;
        int time;

        FlightEdge(String dest, int cost, int time) {
            this.dest = dest;
            this.cost = cost;
            this.time = time;
        }
    }

    // Our graph maps each city name to a list of possible outgoing flights
    private static class Graph {
        private Map<String, List<FlightEdge>> adj = new HashMap<>();

        // Add a flight from origin to dest
        void addEdge(String origin, String dest, int cost, int time) {
            adj.computeIfAbsent(origin, k -> new LinkedList<>())
               .add(new FlightEdge(dest, cost, time));
        }

        // Get all flights you can take starting from this city
        List<FlightEdge> getNeighbors(String city) {
            return adj.getOrDefault(city, Collections.emptyList());
        }
    }

    // Keeps track of one possible route: the cities visited, and totals for cost & time
    private static class Path {
        List<String> cities;
        int cost;
        int time;

        // start a new path from a single city
        Path(String start) {
            this.cities = new ArrayList<>();
            this.cities.add(start);
            this.cost = 0;
            this.time = 0;
        }

        // copy constructor for branching paths
        Path(Path other) {
            this.cities = new ArrayList<>(other.cities);
            this.cost = other.cost;
            this.time = other.time;
        }

        // append a flight edge to this path
        void addEdge(FlightEdge e) {
            cities.add(e.dest);
            cost += e.cost;
            time += e.time;
        }

        // turn the sequence of cities into a printable string
        String getSequence() {
            return String.join(" -> ", cities);
        }
    }

    // A stack frame for iterative backtracking search
    private static class Frame {
        String city;      // current city we are exploring from
        int nextIndex;    // index of which neighbor to visit next
        Path path;        // the route taken so far

        Frame(String city, int nextIndex, Path path) {
            this.city = city;
            this.nextIndex = nextIndex;
            this.path = path;
        }
    }

    public static void main(String[] args) throws IOException {
        // Hard-coded file names in the working directory
        String flightDataFile = "flight_data.txt";
        String requestFile    = "requested_flights.txt";
        String outputFile     = "output.txt";

        // Build our graph of flights
        Graph graph = new Graph();
        try (BufferedReader br = new BufferedReader(new FileReader(flightDataFile))) {
            String line = br.readLine();
            if (line == null) throw new IOException("Empty flight data file");
            int n = Integer.parseInt(line.trim());  // first line = number of legs

            // Read each flight leg and add edges in both directions
            for (int i = 0; i < n; i++) {
                String[] parts = br.readLine().split("\\|");
                String o = parts[0], d = parts[1];
                int c = Integer.parseInt(parts[2]);
                int t = Integer.parseInt(parts[3]);
                graph.addEdge(o, d, c, t);
                graph.addEdge(d, o, c, t);  // make the route bidirectional
            }
        }

        // Process each requested flight and write nicely formatted output
        try (BufferedReader br = new BufferedReader(new FileReader(requestFile));
             PrintWriter out = new PrintWriter(new FileWriter(outputFile))) {

            String line = br.readLine();
            if (line == null) throw new IOException("Empty request file");
            int r = Integer.parseInt(line.trim());  // number of requests

            for (int i = 1; i <= r; i++) {
                String[] parts = br.readLine().split("\\|");
                String o = parts[0], d = parts[1];
                char sortBy = parts[2].charAt(0);

                // Header for this flight request
                out.printf("Flight %d: %s, %s (%s)%n", i, o, d,
                           sortBy=='T' ? "Time" : "Cost");

                // Find all simple paths between origin and destination
                List<Path> paths = findPaths(graph, o, d);
                if (paths.isEmpty()) {
                    out.printf("Path 1: No available flight plan from %s to %s.%n", o, d);
                } else {
                    // Sort ascending by time or cost, pick top 3
                    Comparator<Path> cmp = sortBy=='T'
                        ? Comparator.comparingInt(p -> p.time)
                        : Comparator.comparingInt(p -> p.cost);
                    paths.sort(cmp);
                    int limit = Math.min(3, paths.size());

                    for (int j = 0; j < limit; j++) {
                        Path p = paths.get(j);
                        // Human-friendly output: Path number, city sequence, totals
                        out.printf("Path %d: %s. Time: %d Cost: %d%n",
                                   j+1, p.getSequence(), p.time, p.cost);
                    }
                }

                // Blank line to separate successive flights
                if (i < r) out.println();
            }
        }
    }

    // Iterative DFS/backtracking to gather all simple paths from origin to dest
    private static List<Path> findPaths(Graph graph, String origin, String dest) {
        List<Path> results = new ArrayList<>();
        if (!graph.adj.containsKey(origin)) return results;

        Stack<Frame> stack = new Stack<>();
        stack.push(new Frame(origin, 0, new Path(origin)));

        while (!stack.isEmpty()) {
            Frame f = stack.peek();

            // If we've reached the destination, record this path and backtrack
            if (f.city.equals(dest)) {
                results.add(new Path(f.path));
                stack.pop();
                continue;
            }

            List<FlightEdge> nbrs = graph.getNeighbors(f.city);
            if (f.nextIndex < nbrs.size()) {
                FlightEdge e = nbrs.get(f.nextIndex++);

                // Avoid cycles by ensuring we haven't visited this city yet
                if (!f.path.cities.contains(e.dest)) {
                    Path np = new Path(f.path);
                    np.addEdge(e);
                    stack.push(new Frame(e.dest, 0, np));
                }
            } else {
                // No more neighbors: done exploring from this city, back up
                stack.pop();
            }
        }
        return results;
    }
}
