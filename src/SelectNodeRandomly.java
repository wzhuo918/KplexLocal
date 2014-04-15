import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.StringTokenizer;


public class SelectNodeRandomly {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws NumberFormatException 
	 */
	public static void main(String[] args) throws NumberFormatException, IOException {
		// TODO Auto-generated method stub
		BufferedReader reader = new BufferedReader(new FileReader(args[0]));
		FileWriter out =  new FileWriter(args[0]+"samplenode");
		Double percent = Double.parseDouble(args[1]);
		Set<Integer> nodes = new HashSet<Integer>();
		String line = "";
		while((line=reader.readLine())!=null){
			StringTokenizer st = new StringTokenizer(line);
			while(st.hasMoreElements()){
				nodes.add(Integer.parseInt(st.nextToken()));
			}
		}
		Set<Integer> res = new HashSet<Integer>();
		Integer[] allnodes = nodes.toArray(new Integer[0]);
		while(res.size()<percent/100.0*allnodes.length){
			res.add(allnodes[(int)(Math.random()*allnodes.length)]);
		}
		for(int i:res){
			out.write(i+" ");
		}
		out.flush();
		out.close();
		System.out.println(nodes.size()+" / "+res.size());
	}

}
