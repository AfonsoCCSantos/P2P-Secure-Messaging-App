package models;

import java.util.ArrayList;
import java.util.List;

public class Group {
	
	private String topic;
	private String owner;
	private List<String> members = new ArrayList<>(); 
	
	public Group(String topic, String owner) {
		this.topic = topic;
		this.owner = owner;
		members.add(owner);
	}
	
	public List<String> getMembers() {
		return this.members;
	}

	public void addMembers(String member) {
		this.members.add(member);
	}
}
