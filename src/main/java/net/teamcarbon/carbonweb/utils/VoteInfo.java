package net.teamcarbon.carbonweb.utils;

import com.google.gson.JsonObject;
import com.vexsoftware.votifier.model.Vote;

import java.util.UUID;

public class VoteInfo {

	public boolean done = false;
	private int parseAttempts = 0;
	public final String user, addr, time, serv;

	private UUID uuid;
	private boolean uuidSet = false;

	public VoteInfo(Vote v) {
		user = v.getUsername();
		addr = v.getAddress();
		time = v.getTimeStamp();
		serv = v.getServiceName();
	}
	public synchronized void setUuid(UUID id) {
		if (id == null) return;
		uuid = id;
		uuidSet = true;
	}
	public UUID uuid() { return uuid; }
	public boolean uuidSet() { return uuidSet; }

	public JsonObject asJson() {
		JsonObject obj = new JsonObject();
		if (uuidSet) obj.addProperty("uuid", uuid.toString());
		obj.addProperty("user", user);
		obj.addProperty("address", addr);
		obj.addProperty("time", time);
		obj.addProperty("service", serv);
		return obj;
	}

	public int getParseAttempts() { return parseAttempts; }
	public void incParseAttempts() { parseAttempts++; }
}