import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.*;

public class Trie implements Serializable{
	private int _size;
	private int _highestNodeId;
	private boolean _useFilesystem;
	private String _name;
	private TrieNode root;

	public Trie(String name, boolean useFileSystem) {
		_name = (name == null) ? "" : name.trim();

		root = new TrieNode(0);
		_size = 0;
		_highestNodeId = 0;
		_useFilesystem = useFileSystem;
	}


	public class TrieNode implements Serializable, Comparable<TrieNode> {
		public TreeMap<Character, TrieNode> children = new TreeMap<>();//TreeMap is java build-in structure,
		public boolean aword;			                    //Basically it acts like a Hashtable or Hashmap, establishing a mapping between Key and Value
		public int _id;                                     //Unlike hash table, keys in TreeMap are sorted!

        public boolean hasChildren(){
            return children != null && !children.isEmpty();
        }

		@Override
		public int compareTo(TrieNode that){
			if(that == null){
				throw new NullPointerException();
			}

			return _id - that._id;

		}

		@Override
		public boolean equals(Object that){
			if(!(that instanceof TrieNode)){
				return false;
			}

			return ((TrieNode)that)._id == _id;
		}
		public TrieNode(int id){
			_id = id;
		}
	}// end TrieNode class

	public int insert(String s) {
		return insert(root,s);
	}
	
	public boolean remove(String word){
		if(word == null || word.length() == 0 || !query(word))
			return false;

		Stack<TrieNode> backTrace = new Stack<>();

		TrieNode thisNode = root;
		backTrace.push(root);
		for(char thisChar : word.toCharArray()){
			thisNode = thisNode.children.get(thisChar);
			backTrace.push(thisNode);
		}

		thisNode.aword = false;
		_size--;

		if(_useFilesystem)
			removeNodeFromFs(thisNode);

		backTrace.pop();
		TrieNode parent = backTrace.pop();
		int charIndex = word.length() - 1;

		while(!backTrace.isEmpty()){
			thisNode = parent;
			parent.children.remove(word.charAt(charIndex--));
		if(_useFilesystem)
			writeNodeToFS(thisNode);
			parent = backTrace.pop();

			if(!parent.children.isEmpty())
				break;


		}


		return true;
	}

	private int insert(TrieNode root, String s) {
		TrieNode cur = root;
		for (char ch : s.toCharArray()) {
			TrieNode next = cur.children.get(ch);
			if (next == null)
				cur.children.put(ch, next = createNewNode());
			cur = next;
		}
		cur.aword = true;
		if(_useFilesystem)
			writeNodeToFS(cur);
		return ++_size;
	}

	private TrieNode createNewNode(){
		TrieNode newNode = new TrieNode(_highestNodeId++);

		if(_useFilesystem)
			writeNodeToFS(newNode);

		return newNode;
	}

	private void removeNodeFromFs(TrieNode thisNode){
		if(thisNode == null)
			return;

		try{
			File nodeFile = new File(getNodeFilename(thisNode));
			if(!nodeFile.exists())
				return;

			nodeFile.delete();
		}catch (Exception e){
			System.err.println(e);
		}
	}

	private String getNodeFilename(TrieNode thisNode){
		if(thisNode == null)
			return "";

		return "Trie_" + _name + "_Node_" + thisNode._id + ".ser";
	}

	private void writeNodeToFS(TrieNode outNode){
		if(outNode == null)
			return;
		try{
			FileOutputStream fileOutStream = new FileOutputStream(getNodeFilename(outNode));
			ObjectOutputStream objectOutStream = new ObjectOutputStream(fileOutStream);
			objectOutStream.writeObject(outNode);
			objectOutStream.close();
		}
		catch(Exception e){
			System.err.println(e);
		}
	}

    @Override
	public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
		toString(root, "", stringBuilder);
        return stringBuilder.toString();
	}

	private void toString(TrieNode node, String word, StringBuilder stringBuilder) {
		if (node.aword) {
			stringBuilder.append(word + System.lineSeparator());
		}
		for (Character ch : node.children.keySet()) {
			toString(node.children.get(ch), word + ch, stringBuilder);
		}
	}
	
	public boolean query(String s) {
		return query(root, s);
	}
	
	private boolean query(TrieNode node, String s) {
		if(s != null) {
			String rest = s.substring(1);              //rest is a substring of s, by excluding the first character in s
			char ch = s.charAt(0);                     //ch is the first letter of s
			TrieNode child = node.children.get(ch);	   //return the child that ch associated with. 
			if(s.length() == 1 && child != null)       //if s contains only one letter, and current node has a child associated with that letter, we find the prefix in Trie!
				return true;	                       //base case
			if(child == null)
				return false;
			else
				return query(child, rest);      //recursive, In this way, we follow the path of the trie from _root down towards leaf
		}
		return false;
	}

	public static void main(String[] args){
		Trie thisTrie = new Trie("Test_Trie", true);

		thisTrie.insert("apple");
		thisTrie.insert("ape");
		thisTrie.insert("dog");
		thisTrie.insert("a");
		thisTrie.insert("zebra");
		thisTrie.insert("do");
		thisTrie.insert("doing");

		System.out.println(thisTrie.query("apple"));
		System.out.println(thisTrie.query("ap"));
		System.out.println(thisTrie.query("dog"));
		System.out.println(thisTrie.query("a"));
		System.out.println(thisTrie.query("zebra"));
		System.out.println(thisTrie.query("doing"));
		System.out.println(thisTrie.query("human"));


		System.out.println(thisTrie);
	}
}
