package me.pixelgames.pixelcrack3r.ps.utils;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;

public class FormatHelper {

	public static List<String> nextLine(final String s, final int maxCharsInLine) {
		List<String> text = new ArrayList<>();
		StringBuilder currentLine = new StringBuilder();
		for(int i = 0; i < s.split(" ").length; i++) {
			String word = s.split(" ")[i];
			if(ChatColor.stripColor(word).trim().isEmpty()) continue;
			if(ChatColor.stripColor(word).length() >= maxCharsInLine) {
				text.add(currentLine.substring(0, currentLine.length() - 1));
				text.add(word);
				currentLine = new StringBuilder();
				
			} else {
				if(ChatColor.stripColor(currentLine.toString()).length() + ChatColor.stripColor(word).length() <= maxCharsInLine) {
					currentLine.append(word).append(" ");
				} else {
					text.add(currentLine.substring(0, currentLine.length() - 1));
					currentLine = new StringBuilder();
					currentLine.append(word).append(" ");
				}
			}
			
			if(i >= s.split(" ").length - 1) {
				if(ChatColor.stripColor(currentLine.toString()).length() < 1) continue;
				String line = currentLine.substring(0, currentLine.length() - 1);
				if(line.trim().isEmpty()) continue;
				text.add(line);
			}
		}
		return text;
	}

}
