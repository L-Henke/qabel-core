package de.qabel.core.config;

import java.util.Set;

/** 
 * https://github.com/Qabel/qabel-doc/wiki/Qabel-Client-Contact-Drop-Messages#contacts
 */
public class Contacts extends EntityMap<Contact> {
    /**
	 * Returns unmodifiable set of contained contacts
	 * @return Set<Contact>
	 */
	public Set<Contact> getContacts() {
		return this.getEntities();
	}
}
