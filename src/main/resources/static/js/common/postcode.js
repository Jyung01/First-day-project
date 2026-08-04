document.addEventListener("DOMContentLoaded", function () {
    const searchButtons = document.querySelectorAll("[data-address-search]");

    searchButtons.forEach(function (button) {
        const addressFieldset = button.closest("fieldset");

        if (!addressFieldset) {
            return;
        }

        const postcodeInput = addressFieldset.querySelector('[name="postcode"]');
        const addressInput = addressFieldset.querySelector('[name="address"]');
        const addressDetailInput = addressFieldset.querySelector('[name="addressDetail"]');
        const addressMessage = addressFieldset.querySelector("[data-address-message]");

        if (!postcodeInput || !addressInput) {
            return;
        }

        function showAddressMessage(message) {
            if (!addressMessage) {
                return;
            }

            addressMessage.textContent = message;
            addressMessage.classList.add("is-visible");
        }

        function clearAddressMessage() {
            if (!addressMessage) {
                return;
            }

            addressMessage.textContent = "";
            addressMessage.classList.remove("is-visible");
        }

        button.addEventListener("click", function () {
            if (!window.kakao?.Postcode) {
                showAddressMessage(
                    "주소 검색 서비스를 불러오지 못했습니다. 잠시 후 다시 시도해주세요."
                );
                return;
            }

            clearAddressMessage();

            new window.kakao.Postcode({
                oncomplete: function (data) {
                    const selectedAddress = data.userSelectedType === "R"
                        ? data.roadAddress
                        : data.jibunAddress;

                    postcodeInput.value = data.zonecode;
                    addressInput.value = selectedAddress || data.address;

                    postcodeInput.dispatchEvent(new Event("input", { bubbles: true }));
                    addressInput.dispatchEvent(new Event("input", { bubbles: true }));

                    if (addressDetailInput) {
                        addressDetailInput.value = "";
                        addressDetailInput.focus();
                    }
                },
            }).open();
        });
    });
});
