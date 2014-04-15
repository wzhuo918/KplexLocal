package notwoleapversion;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.Map.Entry;

public class OurRandomVersion {

	// reduce数目
	static int reduceNumber = 36;
	// 有意义的k-plex大小
	static int quasiCliqueSize = 5;
	// k-plex的k值大小
	static int k_plex = 2;
	// 将“一跳”信息读入内存，存在HashMap中
	public static HashMap<Integer, HashSet<Integer>> oneLeap = new HashMap<Integer, HashSet<Integer>>(
			7000);
	// 数据集中的节点集合
	public static HashSet<Integer> nodeSet = new HashSet<Integer>(1000);
	// 子状态集和结果集
	public static Stack<SubGraph> stack = new Stack<SubGraph>();
	

	public static PrintStream ps = null;

	public static void readInOneLeapData(String file) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String line;
		StringTokenizer stk;
		while ((line = reader.readLine()) != null) {
			stk = new StringTokenizer(line);
			int k = Integer.parseInt(stk.nextToken());
			HashSet<Integer> adj = new HashSet<Integer>();
			nodeSet.add(k);
			while (stk.hasMoreTokens()) {
				adj.add(Integer.parseInt(stk.nextToken()));
			}
			oneLeap.put(k, adj);
		}
		reader.close();
	}

	private static SubGraph initSize1SubGraph(Integer current) {
		ArrayList<Pair> tmpres = new ArrayList<Pair>();
		Pair curre = new Pair(current, 0);
		tmpres.add(curre);

		HashMap<Integer, Pair> tmpcand = new HashMap<Integer, Pair>();
		HashMap<Integer, Pair> tmpnot = new HashMap<Integer, Pair>();
		// 生成size-1的子图
		getCandidate2(tmpcand, tmpnot, curre);
		if(tmpcand.size()+1<quasiCliqueSize)
			return null;
		SubGraph initsub = new SubGraph();
		initsub.setCandidate(tmpcand);
		initsub.setNot(tmpnot);
		initsub.setResult(tmpres);
		return initsub;
	}
static int treesize = 0;
static int purningsize = 0;
	public static void computeOneleapData(String file) throws IOException,
			CloneNotSupportedException {
		treesize = 0;
		purningsize = 0;
		cliquenum = 0;
		dupnum = 0;
		nodeSet.clear();
		oneLeap.clear();
		stack.clear();
		readInOneLeapData(file);
		long t1 = System.currentTimeMillis();
		// 排序后，每个reduce只处理对应节点
		for (Integer current : nodeSet) {
//			 if(current==19){
			if (current % reduceNumber == 0) {
//			if (true) {
				SubGraph init = initSize1SubGraph(current);
				if(init == null)
					continue;
				stack.add(init);
				treesize++;
				// "备选集"的概念和kplexold不同，此处备选集包含“两跳”节点，是待分解的原始图
				while (!stack.isEmpty()) {
					SubGraph top = stack.pop();
					ArrayList<Pair> res = top.getResult();

					HashMap<Integer, Pair> candidate = top.getCandidate();
					HashMap<Integer, Pair> not = top.getNot();
					// 这里保证了candidate中的所有点都满足条件2:在临界点邻接表内
					// 这里输入的candidate点都应该满足两个条件,需要在生成时就保证
					// candidate = filterCandidate(res, candidate, not);
					ArrayList<Integer> critnodes = new ArrayList<Integer>();
					getCriticalSet(res, critnodes);// 母图的临界点集合 :母图的临界点必然都是子图的临界点
					
					updateDeg( candidate);
					// 计算res和not的cdeg
					computeDeg(res, candidate);// 这里只是为了一致,size-1图的res的cdeg是已经有了的
					computeDeg(not, candidate);
					while (res.size() + candidate.size() >= quasiCliqueSize) {
						if (candidate.isEmpty()) {
							if (not.isEmpty()) {
//								String r = res.toString();
//								System.out.println(r.substring(1,
//										r.length() - 1));
								cliquenum++;
							} else {
								dupnum++;
							}
							break;
						}
						// 判断not集中是否有点与res和candidate中的点都相邻,以提前剪枝
						if (duplicate(not, res.size(), candidate.size())) {
							dupnum++;
							purningsize++;
							break;
						}
						if (judgeKplex2(res, candidate))// 是kplex
						{
							// 判断not集中是否有点可以和res+candidate构成kplex
							if (duplicate(not, res, candidate))
								break;
							// 是clique输出
//							String r = res.toString();
//							String c = candidate.keySet().toString();
//							System.out.println(r.toString().substring(1,
//									r.length() - 1)
//									+ ", " + c.substring(1, c.length() - 1));
							cliquenum++;
							break;
						} else {
							// 找到度数最小的点
//							Integer yint = deglist.getHead().points.iterator()
//									.next(); 
							Integer yint = candidate.keySet().iterator().next();
							Pair y = candidate.get(yint);

							// 子图包含y
							// res中多了y
							ArrayList<Pair> resA = new ArrayList<Pair>(
									res.size());
							for (Pair tp : res)
								resA.add(tp.clone());

							int nodesize = resA.size() + 1;// 结果集中应该还加上当前分裂点(在后面加上了)
							ArrayList<Integer> tmpcrit = (ArrayList<Integer>) critnodes.clone();
							if (nodesize - y.rdeg == k_plex)
								tmpcrit.add(yint);
							HashSet<Integer> adj = oneLeap.get(yint);
							for (Pair p : resA) {
								if (adj.contains(p.point)) {
									// 与分裂点相邻的点度数加一,这些点要么已经在critnodes中要么不会成为critnode
									p.rdeg++;
								} else if (nodesize - p.rdeg == k_plex) {
									// 与分裂点不相邻的点有可能因为分裂点的加入成为critnode
									tmpcrit.add(p.point);
								}
							}
							resA.add((Pair) y.clone());
							/**
							 * 更新从candidate候选点集合中删除掉当前分裂点y后candidate及res中点的cdeg
							 */
							// 分裂点y从候选点中删除后,需要更新候选集中点的度数值
							// 更新res的cdeg,.更新not的cdeg,.更新candidate的cdeg
							updateMarkDeg(res, not, candidate, y);
							HashMap<Integer, Pair> canA = new HashMap<Integer, Pair>();
							HashMap<Integer, Pair> notA = new HashMap<Integer, Pair>();
							// 用临界点条件2和条件1从候选点集合中选出满足条件的点作为切出来的子图的候选点
							// 这里要注意维护各个集合点的rdeg
							filterCandidate(canA, notA, tmpcrit, candidate,
									not, adj, nodesize);
							if (canA.size() + resA.size() >= quasiCliqueSize) {
								SubGraph sA = new SubGraph();
								sA.setCandidate(canA);
								sA.setResult(resA);
								sA.setNot(notA);
								stack.add(sA);
								treesize++;
							}

							not.put(yint, y);
						}
					}
				}
			}
		}
		long t2 = System.currentTimeMillis();
		System.out.println("kplex num="+cliquenum+"========" + (t2 - t1) / 1000+" s, treesize="+treesize+
				"/purningsize:"+purningsize);
	}

	private static boolean duplicate(HashMap<Integer, Pair> not,
			ArrayList<Pair> res, HashMap<Integer, Pair> candidate) {
		ArrayList<Integer> critnodes = new ArrayList<Integer>();
		int size = res.size() + candidate.size() - k_plex;
		for (Pair c : candidate.values()) {
			if (c.cdeg + c.rdeg == size)
				critnodes.add(c.point);
		}
		for (Pair c : res) {
			if (c.cdeg + c.rdeg == size)
				critnodes.add(c.point);
		}
		if (!critnodes.isEmpty()) {
			HashSet<Integer> intersection = new HashSet<Integer>();
			intersection.addAll(oneLeap.get(critnodes.get(0)));// 先加入第一个元素
			for (int i = 1; i < critnodes.size(); i++) {
				intersection.retainAll(oneLeap.get(critnodes.get(i)));// 不断取交集
			}
			if (not.size() < intersection.size()) {
				Iterator<Integer> it = not.keySet().iterator();
				while (it.hasNext()) {
					if (!intersection.contains(it.next()))
						it.remove();
				}
				for (Pair n : not.values()) {
					if (n.cdeg + n.rdeg <= size)
						continue;
					return true;
				}
			} else {
				for (Integer i : intersection) {
					Pair p = not.get(i);
					if (p != null) {
						if (p.cdeg + p.rdeg <= size)
							continue;
						return true;
					}
				}
			}
		} else {
			for (Pair n : not.values()) {
				if (n.cdeg + n.rdeg <= size)
					continue;
				return true;
			}
		}
		return false;
	}

	/**
	 * 从候选点集合中删除一个点y,加入到新子图中的结果集中 那么现有的候选集及结果集包括not集都需要更新这个点从候选集中删除后所带来的cdeg的影响
	 * 
	 * @param res
	 * @param not
	 * @param candidate
	 * @param deglist
	 * @param y
	 */
	private static void updateMarkDeg(ArrayList<Pair> res,
			HashMap<Integer, Pair> not, HashMap<Integer, Pair> candidate,
			 Pair y) {
		HashSet<Integer> adj = oneLeap.get(y.point);
		// 点y加入结果集中,导致res和not集中与y相邻的点的度数减1
		for (Pair r : res) {
			if (adj.contains(r.point))
				r.cdeg--;
		}
		if (not.size() < adj.size()) {
			for (Pair n : not.values()) {
				if (adj.contains(n.point))
					n.cdeg--;
			}
		} else {
			Pair p;
			for (Integer a : adj) {
				p = not.get(a);
				if (p != null) {
					p.cdeg--;
				}
			}
		}

		candidate.remove(y.point);

		if (candidate.size() < adj.size()) {
			for (Pair n : candidate.values()) {
				if (adj.contains(n.point))
					n.cdeg--;
			}
		} else {
			Pair p;
			for (Integer a : adj) {
				p = candidate.get(a);
				if (p != null) {
					p.cdeg--;
				}
			}
		}

	}

	private static void computeDeg(Map<Integer, Pair> res,
			HashMap<Integer, Pair> candidate) {
		int num;
		for (Entry<Integer, Pair> p : res.entrySet()) {
			num = 0;
			HashSet<Integer> adj = oneLeap.get(p.getKey());
			if (adj.size() < candidate.size()) {
				for (Integer a : adj) {
					if (candidate.containsKey(a))
						num++;
				}
			} else {
				for (Integer c : candidate.keySet()) {
					if (adj.contains(c))
						num++;
				}
			}
			p.getValue().cdeg = num;
		}
	}

	private static void computeDeg(ArrayList<Pair> res,
			HashMap<Integer, Pair> candidate) {
		int num;
		for (Pair p : res) {
			num = 0;
			HashSet<Integer> adj = oneLeap.get(p.point);
			if (adj.size() < candidate.size()) {
				for (Integer a : adj) {
					if (candidate.containsKey(a))
						num++;
				}
			} else {
				for (Integer c : candidate.keySet()) {
					if (adj.contains(c))
						num++;
				}
			}
			p.cdeg = num;
		}
	}

	private static void updateDeg(
			HashMap<Integer, Pair> candidate) {
		for (Pair ca : candidate.values())// oneLeap中节点和candidate中节点的交集数
		{
			int number = 0;
			HashSet<Integer> adj = oneLeap.get(ca.point);
			if (candidate.size() < adj.size()) {
				for (Integer out : candidate.keySet()) {
					if (adj.contains(out))
						number++;
				}
			} else {
				for (Integer a : adj) {
					if (candidate.containsKey(a))
						number++;
				}
			}
			ca.cdeg = number;
		}
	}

	private static boolean duplicate(HashMap<Integer, Pair> not, int rsize,
			int csize) {
		for (Pair p : not.values()) {
			if (p.rdeg == rsize && p.cdeg == csize)
				return true;
		}
		return false;
	}

	public static void init(String[] args) throws NumberFormatException,
			IOException {
//		ps = new PrintStream(new File(args[1]));
//		System.setOut(ps);
		reduceNumber = Integer.parseInt(args[1]);
		quasiCliqueSize = 5;
		k_plex = Integer.parseInt(args[2]);
	}

	/**
	 * 生成初始的size-1子图,子图中点的rdeg度数以通过是一跳数据还是两跳数据记录为0和1
	 * 子图中candidate点的cdeg由于未计算都设置为默认值-1
	 * 
	 * @param candidate
	 * @param not
	 * @param current
	 */
	public static void getCandidate2(HashMap<Integer, Pair> candidate,
			HashMap<Integer, Pair> not, Pair current) {

		int tmpdeg = 0;
		final HashSet<Integer> oneadj;
		oneadj = oneLeap.get(current.point);
		int curdeg = oneadj.size();
		current.cdeg = curdeg;
		HashSet<Integer> twoadj;

		HashSet<Integer>tmpadj;
		// 缓存未命中
		twoadj = new HashSet<Integer>();
		for (Integer i : oneadj) {// 一跳集
			tmpadj = oneLeap.get(i);
			twoadj.addAll(tmpadj);// 生成二跳集
			tmpdeg = tmpadj.size();
			if (tmpdeg > curdeg) {
				not.put(i, new Pair(i, 1));
			} else if (tmpdeg < curdeg) {
				candidate.put(i, new Pair(i, 1));
			} else {
				if (i > current.point) {
					candidate.put(i, new Pair(i, 1));
				} else
					// if (i < current) {//一跳集中不会有current本身
					not.put(i, new Pair(i, 1));
				// }// 相等的话即current本身,不需要加入任何集合,不处理
			}
		}
		twoadj.removeAll(oneadj);// 仅包含第二跳集
		twoadj.remove(current.point);
		for (Integer i : twoadj) {
			tmpdeg = oneLeap.get(i).size();
			if (tmpdeg > curdeg) {
				not.put(i, new Pair(i, 0));
			} else if (tmpdeg < curdeg) {
				candidate.put(i, new Pair(i, 0));
			} else {
				if (i > current.point) {
					candidate.put(i, new Pair(i, 0));
				} else
					// if (i < current) {//上面保证了二跳集中不会有current
					not.put(i, new Pair(i, 0));
			}
		}
	}

	/**
	 * candidate的度数记录某个点和candidate中其他点相邻的个数
	 * 
	 * @param candidate
	 */
	public static void initDegree(ArrayList<Pair> candidate) {

		for (Pair in : candidate)// oneLeap中节点和candidate中节点的交集数
		{
			int number = 0;
			HashSet<Integer> adj = oneLeap.get(in.point);
			for (Pair out : candidate) {
				if (!adj.contains(out.point) && in.point != out.point)
					number++;
			}
			in.cdeg = number;
		}
	}

	public static void getCriticalSet(List<Pair> res, List<Integer> critSet) {
		int size = res.size();
		if (size <= k_plex)
			return;
		size -= k_plex;
		for (Pair p : res) {
			if (p.rdeg == size)
				critSet.add(p.point);
		}
	}

	/**
	 * 通过临界点集合和条件1来过滤候选点和not集中可以加入到新子图中的点 同时更新加入到子图中点的rdeg值,因为子图结果集中比母图多一个分裂点
	 * 
	 * @param canA
	 * @param notA
	 * @param critSet
	 * @param candidate
	 * @param not
	 * @param adj
	 * @throws CloneNotSupportedException
	 */
	private static void filterCandidate(HashMap<Integer, Pair> canA,
			HashMap<Integer, Pair> notA, ArrayList<Integer> critSet,
			HashMap<Integer, Pair> candidate, HashMap<Integer, Pair> not,
			HashSet<Integer> yadj, int ressize)
			throws CloneNotSupportedException {
		if (!critSet.isEmpty()) {
			HashSet<Integer> intersection = new HashSet<Integer>();
			intersection.addAll(oneLeap.get(critSet.get(0)));// 先加入第一个元素
			for (int i = 1; i < critSet.size(); i++) {
				intersection.retainAll(oneLeap.get(critSet.get(i)));// 不断取交集
			}
			// 用临界点过滤candidate
			if (intersection.size() < candidate.size()) {
				for (Integer i : intersection) {
					Pair p = candidate.get(i);
					if (p != null) {
						canA.put(i, p.clone());
					}
				}
			} else {
				for (Pair p : candidate.values()) {
					if (intersection.contains(p.point))
						canA.put(p.point, p.clone());
				}
			}
			// 用临界点过滤not
			if (intersection.size() < not.size()) {
				for (Integer i : intersection) {
					Pair p = not.get(i);
					if (p != null) {
						notA.put(i, p.clone());
					}
				}
			} else {
				for (Pair p : not.values()) {
					if (intersection.contains(p.point))
						notA.put(p.point, p.clone());
				}
			}
		} else {
			for (Entry<Integer, Pair> en : candidate.entrySet())
				canA.put(en.getKey(), en.getValue().clone());
			for (Entry<Integer, Pair> en : not.entrySet())
				notA.put(en.getKey(), en.getValue().clone());
		}
		// 更新集合中点的rdeg,包含在adj中点的rdeg++,同时过滤掉其中不满足条件1的点
		updateRdeg(canA, yadj, ressize);
		updateRdeg(notA, yadj, ressize);
	}

	/**
	 * 更新集合中点的rdeg,包含在adj中点的rdeg++,同时过滤掉其中不满足条件1的点
	 * 
	 * @param can
	 * @param yadj
	 * @param ressize
	 */
	private static void updateRdeg(HashMap<Integer, Pair> can,
			HashSet<Integer> adj, int ressize) {
		Iterator<Entry<Integer, Pair>> it = can.entrySet().iterator();
		int mindeg = ressize - k_plex + 1;
		while (it.hasNext()) {
			Pair p = it.next().getValue();
			if (adj.contains(p.point))
				p.rdeg++;
			else if (p.rdeg < mindeg) {
				it.remove();
			}
		}
	}

	/**
	 * 遍历candidate和res中所有点判断是不是所有点的rdeg+cdeg都满足kplex条件
	 * 
	 * @param res
	 * @param candidate
	 * @param critnodes
	 * @return
	 */
	public static boolean judgeKplex2(ArrayList<Pair> res,
			HashMap<Integer, Pair> candidate) {
		int size = res.size() + candidate.size() - k_plex;
		for (Pair r : res) {
			if (r.rdeg + r.cdeg < size)
				return false;
		}
		for (Pair c : candidate.values()) {
			if (c.rdeg + c.cdeg < size)
				return false;
		}
		return true;
	}

	static int cliquenum = 0;
	static int dupnum = 0;

	/**
	 * @param args
	 * @throws IOException
	 * @throws CloneNotSupportedException
	 */
	public static void main(String[] args) throws IOException,
			CloneNotSupportedException {
		init(args);
		computeOneleapData(args[0]);
		// testKplex(args);
	}
}
