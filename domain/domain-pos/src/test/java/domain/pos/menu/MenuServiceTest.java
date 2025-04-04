package domain.pos.menu;

import static fixtures.member.OwnerFixture.*;
import static fixtures.menu.MenuCategoryFixture.*;
import static fixtures.menu.MenuFixture.*;
import static fixtures.menu.MenuInfoFixture.*;
import static fixtures.store.StoreFixture.*;
import static fixtures.store.StoreInfoFixture.*;
import static org.assertj.core.api.SoftAssertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import com.exception.ErrorCode;
import com.exception.ServiceException;

import base.ServiceTest;
import domain.pos.menu.entity.Menu;
import domain.pos.menu.entity.MenuCategory;
import domain.pos.menu.entity.MenuInfo;
import domain.pos.menu.implement.MenuCategoryValidator;
import domain.pos.menu.implement.MenuReader;
import domain.pos.menu.implement.MenuWriter;
import domain.pos.menu.service.MenuService;
import domain.pos.store.entity.Store;
import domain.pos.store.entity.StoreInfo;
import domain.pos.store.implement.StoreReader;
import domain.pos.store.implement.StoreValidator;

public class MenuServiceTest extends ServiceTest {
	@Mock
	private StoreValidator storeValidator;
	@Mock
	private MenuCategoryValidator menuCategoryValidator;
	@Mock
	private StoreReader storeReader;
	@Mock
	private MenuReader menuReader;
	@Mock
	private MenuWriter menuWriter;
	@InjectMocks
	private MenuService menuService;

	@Nested
	@DisplayName("메뉴 생성")
	class postMenu {
		private final Long storeId = 1L;
		private final Long userId = 2L;
		private final Long menuCategoryId = 3L;
		private final MenuInfo requestMenuInfo = REQUEST_MENU_INFO();

		private final Long menuId = 3L;

		@Test
		void 메뉴_생성_성공() {
			// given
			StoreInfo storeInfo = GENERAL_STORE_INFO();
			MenuCategory menuCategory = CUSTOM_MENU_CATEGORY(menuCategoryId);
			Menu menu = CUSTOM_MENU(REQUEST_TO_ENTITY(menuId, requestMenuInfo), storeInfo, menuCategory);

			BDDMockito.given(menuWriter.postMenu(storeId, userId, menuCategoryId, requestMenuInfo))
				.willReturn(menu);

			// when
			Menu serviceMenu = menuService.postMenu(storeId, userId, menuCategoryId, requestMenuInfo);

			// then
			assertSoftly(softly -> {
				MenuInfo serviceMenuInfo = serviceMenu.getMenuInfo();
				StoreInfo serviceStoreInfo = serviceMenu.getStoreInfo();
				MenuCategory serviceMenuCategory = serviceMenu.getMenuCategory();

				softly.assertThat(serviceMenuInfo.getMenuId()).isEqualTo(menuId);
				softly.assertThat(serviceMenuInfo.getMenuName()).isEqualTo(requestMenuInfo.getMenuName());
				softly.assertThat(serviceMenuInfo.getPrice()).isEqualTo(requestMenuInfo.getPrice());
				softly.assertThat(serviceMenuInfo.getDescription()).isEqualTo(requestMenuInfo.getDescription());
				softly.assertThat(serviceMenuInfo.getImageUrl()).isEqualTo(requestMenuInfo.getImageUrl());

				softly.assertThat(serviceStoreInfo.getStoreId()).isEqualTo(storeInfo.getStoreId());

				softly.assertThat(serviceMenuCategory.getMenuCategoryId()).isEqualTo(menuCategory.getMenuCategoryId());
			});
		}

		@Test
		void 주점_조회_실패() {
			// given
			doThrow(new ServiceException(ErrorCode.NOT_FOUND_STORE))
				.when(storeValidator)
				.validateStoreOwner(storeId, userId);

			// when -> then
			assertSoftly(softly -> {
				softly.assertThatThrownBy(() -> menuService.postMenu(storeId, userId, menuCategoryId, requestMenuInfo))
					.isInstanceOf(ServiceException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND_STORE);
				verify(storeValidator)
					.validateStoreOwner(storeId, userId);
				verify(menuCategoryValidator, never())
					.validateMenuCategory(menuCategoryId);
				verify(menuWriter, never())
					.postMenu(storeId, userId, menuCategoryId, requestMenuInfo);
			});
		}

		@Test
		void 점주_인증_실패() {
			// given
			doThrow(new ServiceException(ErrorCode.NOT_EQUAL_STORE_OWNER))
				.when(storeValidator)
				.validateStoreOwner(storeId, userId);

			// when -> then
			assertSoftly(softly -> {
				softly.assertThatThrownBy(() -> menuService.postMenu(storeId, userId, menuCategoryId, requestMenuInfo))
					.isInstanceOf(ServiceException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_EQUAL_STORE_OWNER);
				verify(storeValidator)
					.validateStoreOwner(storeId, userId);
				verify(menuCategoryValidator, never())
					.validateMenuCategory(menuCategoryId);
				verify(menuWriter, never())
					.postMenu(storeId, userId, menuCategoryId, requestMenuInfo);
			});
		}

		@Test
		void 메뉴_카테고리_조회_실패() {
			// given
			doThrow(new ServiceException(ErrorCode.MENU_CATEGORY_NOT_FOUND))
				.when(menuCategoryValidator)
				.validateMenuCategory(menuCategoryId);

			// when -> then
			assertSoftly(softly -> {
				softly.assertThatThrownBy(() -> menuService.postMenu(storeId, userId, menuCategoryId, requestMenuInfo))
					.isInstanceOf(ServiceException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.MENU_CATEGORY_NOT_FOUND);
				verify(storeValidator)
					.validateStoreOwner(storeId, userId);
				verify(menuCategoryValidator)
					.validateMenuCategory(menuCategoryId);
				verify(menuWriter, never())
					.postMenu(storeId, userId, menuCategoryId, requestMenuInfo);
			});
		}
	}

	@Nested
	@DisplayName("메뉴 리스트 조회")
	class getMenuSlice {
		private final int size = 10;
		private final boolean hasNext = false;
		private final Pageable pageable = Pageable.ofSize(size);
		private final Long lastMenuId = 10L;
		private final Long storeId = 1L;
		private final Long menuCategoryId = 1L;

		private final Long userId = 1L;

		@Test
		void 메뉴_리스트_조회_성공() {
			// given
			Store store = CUSTOM_STORE(storeId, CUSTOM_STORE_INFO(storeId), CUSTOM_OWNER(userId));
			MenuInfo lastMenuInfo = CUSTOM_MENU_INFO(lastMenuId);
			MenuInfo nextMenuInfo = CUSTOM_MENU_INFO(lastMenuId + 1);
			Slice<MenuInfo> menuSlice = new SliceImpl<>(new ArrayList<>(List.of(nextMenuInfo)), pageable, hasNext);

			BDDMockito.given(storeReader.readSingleStore(storeId))
				.willReturn(Optional.of(store));
			BDDMockito.given(menuReader.getMenuInfo(lastMenuId))
				.willReturn(Optional.of(lastMenuInfo));
			BDDMockito.given(menuReader.getMenuSlice(pageable, lastMenuInfo, menuCategoryId))
				.willReturn(menuSlice);

			// when
			Slice<MenuInfo> serviceMenuSlice = menuService.getMenuSlice(pageable, lastMenuId, storeId, menuCategoryId);

			// then
			assertSoftly(softly -> {
				softly.assertThat(serviceMenuSlice.getSize()).isEqualTo(size);
				softly.assertThat(serviceMenuSlice.hasNext()).isEqualTo(hasNext);
				softly.assertThat(serviceMenuSlice.getContent().get(0).getMenuId()).isEqualTo(lastMenuId + 1);
			});
		}

		@Test
		void 주점_조회_실패() {
			// given
			BDDMockito.given(storeReader.readSingleStore(storeId))
				.willReturn(Optional.empty());

			// when -> then
			assertSoftly(softly -> {
				softly.assertThatThrownBy(
						() -> menuService.getMenuSlice(pageable, lastMenuId, storeId, menuCategoryId))
					.isInstanceOf(ServiceException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND_STORE);
				verify(storeReader)
					.readSingleStore(storeId);
				verify(menuCategoryValidator, never())
					.validateMenuCategory(menuCategoryId);
				verify(menuReader, never())
					.getMenuInfo(lastMenuId);
				verify(menuReader, never())
					.getMenuSlice(any(Pageable.class), any(MenuInfo.class), any(Long.class));
			});
		}

		@Test
		void 카테고리_조회_실패() {
			// given
			Store store = CUSTOM_STORE(storeId, CUSTOM_STORE_INFO(storeId), CUSTOM_OWNER(userId));
			BDDMockito.given(storeReader.readSingleStore(any(Long.class)))
				.willReturn(Optional.of(store));

			doThrow(new ServiceException(ErrorCode.MENU_CATEGORY_NOT_FOUND))
				.when(menuCategoryValidator)
				.validateMenuCategory(menuCategoryId);

			// when -> then
			assertSoftly(softly -> {
				softly.assertThatThrownBy(
						() -> menuService.getMenuSlice(pageable, lastMenuId, storeId, menuCategoryId))
					.isInstanceOf(ServiceException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.MENU_CATEGORY_NOT_FOUND);
				verify(storeReader)
					.readSingleStore(storeId);
				verify(menuCategoryValidator)
					.validateMenuCategory(menuCategoryId);
				verify(menuReader, never())
					.getMenuInfo(lastMenuId);
				verify(menuReader, never())
					.getMenuSlice(any(Pageable.class), any(MenuInfo.class), any(Long.class));
			});
		}

		@Test
		void 메뉴_조회_실패() {
			// given
			Store store = CUSTOM_STORE(storeId, CUSTOM_STORE_INFO(storeId), CUSTOM_OWNER(userId));
			BDDMockito.given(storeReader.readSingleStore(any(Long.class)))
				.willReturn(Optional.of(store));
			BDDMockito.given(menuReader.getMenuInfo(any(Long.class)))
				.willReturn(Optional.empty());

			// when -> then
			assertSoftly(softly -> {
				softly.assertThatThrownBy(
						() -> menuService.getMenuSlice(pageable, lastMenuId, storeId, menuCategoryId))
					.isInstanceOf(ServiceException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.MENU_NOT_FOUND);
				verify(storeReader)
					.readSingleStore(storeId);
				verify(menuCategoryValidator)
					.validateMenuCategory(menuCategoryId);
				verify(menuReader)
					.getMenuInfo(lastMenuId);
				verify(menuReader, never())
					.getMenuSlice(any(Pageable.class), any(MenuInfo.class), any(Long.class));
			});
		}
	}
}
