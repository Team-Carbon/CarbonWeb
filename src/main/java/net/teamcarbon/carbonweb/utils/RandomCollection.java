package net.teamcarbon.carbonweb.utils;

import org.bukkit.Bukkit;

import java.util.NavigableMap;
import java.util.Random;
import java.util.TreeMap;

public class RandomCollection<E> {
	private final NavigableMap<Double, E> map = new TreeMap<>();
	private final Random random;
	private double total = 0;

	public RandomCollection() {
		this(new Random());
	}

	public RandomCollection(Random random) {
		this.random = random;
	}

	public void add(double weight, E result) {
		if (weight <= 0) weight = 1;
		total += weight;
		map.put(total, result);
	}

	public E next() {
		double value = random.nextDouble() * total;
		if (map == null) { Bukkit.getLogger().warning("map == null"); }
		if (map.ceilingEntry(value) == null) { Bukkit.getLogger().warning("entry == null"); }
		return map.ceilingEntry(value).getValue();
	}

	public void clear() {
		map.clear();
		total = 0;
	}
}