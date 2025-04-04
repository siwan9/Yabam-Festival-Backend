package domain.pos.store.service;

import org.springframework.stereotype.Service;

import com.exception.ErrorCode;
import com.exception.ServiceException;

import domain.pos.member.entity.Owner;
import domain.pos.member.implement.OwnerReader;
import domain.pos.store.entity.Store;
import domain.pos.store.entity.StoreInfo;
import domain.pos.store.implement.StoreReader;
import domain.pos.store.implement.StoreWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class StoreService {
	private final OwnerReader ownerReader;
	private final StoreWriter storeWriter;
	private final StoreReader storeReader;

	public Long createStore(final Long ownerId, final StoreInfo createRequestStoreInfo) {
		Owner owner = ownerReader.findOwner(ownerId)
			.orElseThrow(() -> {
				log.warn("점주 조회 실패: ownerId={}", ownerId);
				throw new ServiceException(ErrorCode.NOT_VALID_OWNER);
			});

		final Long savedStoreId = storeWriter.createStore(owner, createRequestStoreInfo);
		log.info("가게 생성 성공 : ownerId={}, storeId={}", ownerId, savedStoreId);
		return savedStoreId;
	}

	public Store findStore(final Long storeId) {
		log.info("가게 조회: storeId={}", storeId);
		return storeReader.readSingleStore(storeId)
			.orElseThrow(() -> {
				log.warn("가게 조회 실패: storeId={}", storeId);
				throw new ServiceException(ErrorCode.NOT_FOUND_STORE);
			});
	}

	public Store updateStoreInfo(
		final Long ownerId,
		final Long queryStoreId,
		final StoreInfo requestChangeStoreInfo) {
		ownerReader.findOwner(ownerId)
			.orElseThrow(() -> {
				log.warn("점주 조회 실패: ownerId={}", ownerId);
				throw new ServiceException(ErrorCode.NOT_VALID_OWNER);
			});

		final Store previousStore = storeReader.readSingleStore(queryStoreId)
			.orElseThrow(() -> {
				log.warn("가게 조회 실패: storeId={}", queryStoreId);
				throw new ServiceException(ErrorCode.NOT_FOUND_STORE);
			});

		if (isEqualSavedStoreOwnerAndQueryOwner(ownerId, previousStore)) {
			log.warn("수정 요청 실패: ownerId={}, queryStoreId={}", ownerId, queryStoreId);
			throw new ServiceException(ErrorCode.NOT_EQUAL_STORE_OWNER);
		}

		Store updatedStore = storeWriter
			.updateStoreInfo(previousStore, requestChangeStoreInfo);
		log.info("가게 정보 수정 성공 : ownerId={}, storeId={}", ownerId, queryStoreId);

		return updatedStore;
	}

	// 가게 소유자와 요청 점주가 같은지 확인
	private boolean isEqualSavedStoreOwnerAndQueryOwner(Long ownerId, Store previousStore) {
		return !previousStore.getStoreOwner().getOwnerId().equals(ownerId);
	}

	public void deleteStore(Long ownerId, Long storeId) {
		ownerReader.findOwner(ownerId)
			.orElseThrow(() -> {
				log.warn("점주 조회 실패: ownerId={}", ownerId);
				throw new ServiceException(ErrorCode.NOT_VALID_OWNER);
			});
		final Store previousStore = storeReader.readSingleStore(storeId)
			.orElseThrow(() -> {
				log.warn("가게 조회 실패: storeId={}", storeId);
				throw new ServiceException(ErrorCode.NOT_FOUND_STORE);
			});
		if (isEqualSavedStoreOwnerAndQueryOwner(ownerId, previousStore)) {
			log.warn("삭제 요청 실패: ownerId={}, queryStoreId={}", ownerId, storeId);
			throw new ServiceException(ErrorCode.NOT_EQUAL_STORE_OWNER);
		}
		storeWriter.deleteStore(previousStore);
		log.info("가게 삭제 성공 : ownerId={}, storeId={}", ownerId, storeId);
	}
}
