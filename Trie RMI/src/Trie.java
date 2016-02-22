import java.util.*;

public class Trie {
	
	private class TrieNode {
		Map<Character, TrieNode> children = new TreeMap<>();//TreeMap is java build-in structure, 
		boolean aword;			                    //Basically it acts like a Hashtable or Hashmap, establishing a mapping between Key and Value
		                                                    //Unlike hash table, keys in TreeMap are sorted!
	}
	
	private TrieNode root;
	public Trie() {
		this.root = new TrieNode();
	}

	public void insertString(String s) {
		insertString(root, s);
	}
	
	private void insertString(TrieNode root, String s) {
		TrieNode cur = root;
		for (char ch : s.toCharArray()) {
			TrieNode next = cur.children.get(ch);
			if (next == null)
				cur.children.put(ch, next = new TrieNode());
			cur = next;
		}
		cur.aword = true;
	}
	
	public void printSorted() {
		printSorted(root, "");
	}

	private void printSorted(TrieNode node, String s) {
		if (node.aword) {
			System.out.println(s);
		}
		for (Character ch : node.children.keySet()) {
			printSorted(node.children.get(ch), s + ch);
		}
	}
	
	public boolean findWord(String s) {
		return findWord(root, s);
	}
	
	private boolean findWord(TrieNode node, String s) {
		if(s != null) {
			String rest = s.substring(1);              //rest is a substring of s, by excluding the first character in s
			char ch = s.charAt(0);                     //ch is the first letter of s
			TrieNode child = node.children.get(ch);	   //return the child that ch associated with. 
			if(s.length() == 1 && child != null)       //if s contains only one letter, and current node has a child associated with that letter, we find the prefix in Trie!
				return true;	                   //base case
			if(child == null)
				return false;
			else
				return findWord(child, rest);      //recursive, In this way, we follow the path of the trie from root down towards leaf
		}
		return false;
	}
}
