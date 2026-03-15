package com.fortnite.pronos.repository;

import org.springframework.data.repository.Repository;

import com.fortnite.pronos.model.PrSnapshot;

public interface PrSnapshotNativeRepository
    extends Repository<PrSnapshot, PrSnapshot.PrSnapshotId> {

  void persist(PrSnapshot snapshot);
}
