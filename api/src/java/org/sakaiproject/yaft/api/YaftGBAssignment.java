package org.sakaiproject.yaft.api;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * This is a json friendly, sparse representation of a Gradebook assignment object.
 */
@AllArgsConstructor @Getter
public class YaftGBAssignment {
	
	public Long id;
	public String name;
}
