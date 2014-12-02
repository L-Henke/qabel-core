package de.qabel.core.config;

import org.meanbean.lang.Factory;

/**
 * StorageVolumesTestFactory
 * Creates distinct instances of class StorageVolumes
 * Attention: For testing purposes only!
 */
class StorageVolumesTestFactory implements Factory<StorageVolumes>{
	StorageVolumeTestFactory volumeFactory;
	StorageVolumesTestFactory () {
		volumeFactory = new StorageVolumeTestFactory();
	}
	
	@Override
	public StorageVolumes create() {
		StorageVolumes storageVolumes = new StorageVolumes();

		storageVolumes.add(volumeFactory.create());
		storageVolumes.add(volumeFactory.create());
		
		return storageVolumes;
	}
}
