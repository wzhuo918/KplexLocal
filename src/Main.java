import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;


public class Main {

//	public static void main(String[] args) throws IOException, CloneNotSupportedException {
//		// TODO Auto-generated method stub
//		BufferedReader reader = new BufferedReader(new FileReader("InputData"));
//		PrintStream ps = new PrintStream("result");
//		System.setOut(ps);
//		String prefix = "/home/youli/dataset/oneleapdata/";
//		String line = null;
//		while((line=reader.readLine())!=null){
//			String[] strs = line.split("\t");
//			strs[0] = prefix+strs[0];
//
//			System.out.println("Binary :"+strs[0]);
//			kplexnotwoleap.main(strs);
//			ps.flush();
//			
//			System.out.println("Origin :"+strs[0]);
//			MyOptimize.main(strs);
//			ps.flush();
//		}
//		ps.close();
//	}
	public static void main(String[]args) throws NumberFormatException, IOException{
		Map<Integer,HashSet<Integer>> vedge = new HashMap<Integer,HashSet<Integer>>();
		String input = args[0];
		String output = args[1];
		BufferedReader reader = new BufferedReader(new FileReader(input));
		String line = "";
		while((line=reader.readLine()) != null){
			StringTokenizer st = new StringTokenizer(line);
			int k = Integer.parseInt(st.nextToken());
			int v = Integer.parseInt(st.nextToken());
			if(k==v)
				continue;
			HashSet<Integer> adj = vedge.get(k);
			if(adj==null){
				adj = new HashSet<Integer>();
				vedge.put(k, adj);
			}
			adj.add(v);
			
			
			HashSet<Integer> vadj = vedge.get(v);
			if(vadj==null){
				vadj = new HashSet<Integer>();
				vedge.put(v, vadj);
			}
			vadj.add(k);
		}
		reader.close();
		FileWriter fw = new FileWriter(output);
		for(Entry<Integer,HashSet<Integer>> en:vedge.entrySet()){
			fw.write(""+en.getKey());
			for(Integer v:en.getValue())
				fw.write(" " + v);
			fw.write("\n");
		}
		fw.close();
	}
}
