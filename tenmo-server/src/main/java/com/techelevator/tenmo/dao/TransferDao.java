package com.techelevator.tenmo.dao;

import com.techelevator.tenmo.model.Transfer;

import java.util.List;

public interface TransferDao {

    Transfer getTransfer(long transferId);

    List<Transfer> getAllTransfersByUserID(long userId);

    Transfer createTransfer(Transfer transfer);

    boolean updateTransfer(Transfer transfer);

}
