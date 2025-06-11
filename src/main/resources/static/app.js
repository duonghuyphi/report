var app = angular.module("reportApp", []);

//var API_BASE_URL = "http://localhost:8080";
var API_BASE_URL = 'https://report-1pjd.onrender.com';

app.controller("ReportController", function ($scope, $http) {
    $scope.tables = [];
    $scope.tableData = [];
    $scope.columns = [];
    $scope.selectedTable = "";
    $scope.filters = {}; // Để lưu các bộ lọc cho các cột
    $scope.columnVisibility = {}; // Để lưu trạng thái hiển thị của các cột
    $scope.isHidden = true;
    $scope.flag = false;

    // Khởi tạo tất cả đều được hiển thị
    $scope.columns.forEach(col => {
        $scope.columnVisibility[col] = true;
    });

    $scope.selectAllColumns = false;

// Hàm toggle tất cả
    $scope.toggleAllColumns = function () {
        $scope.columns.forEach(col => {
            $scope.columnVisibility[col] = $scope.selectAllColumns;
        });
    };

    $scope.toggleTables = function () {
        $scope.isHidden = !$scope.isHidden;
    };

    // Load table list
    $http.get(API_BASE_URL + "/tables").then(function (response) {
        $scope.tables = response.data.filter((value, index, self) => {
            const isDuplicate = self.indexOf(value) !== index;
            const isSystemTable =
                value.includes("wp_") || value.includes("pma_");
            return !isDuplicate && !isSystemTable;
        });
    });

    // Load data of selected table
    $scope.viewTable = function (tableName) {
        $scope.flag = true;
        $scope.selectedTable = tableName;
        const apiUrl = API_BASE_URL + `/table-data?tableName=${encodeURIComponent(tableName)}`;
        $http
            .get(apiUrl)
            .then(function (response) {
                $scope.tableData = response.data;
                if ($scope.tableData.length > 0) {
                    $scope.columns = Object.keys($scope.tableData[0]);
                    // Mặc định tất cả các cột đều hiển thị
                    angular.forEach($scope.columns, function (col) {
                        $scope.columnVisibility[col] = true;
                    });
                } else {
                    $scope.columns = [];
                }
            })
            .catch(function (error) {
                console.error("Lỗi tải dữ liệu bảng:", error);
                alert("Không thể tải dữ liệu bảng.");
            });
    };

    // Xóa bảng
    $scope.deleteTable = function (tableName) {
        if (!confirm(`Bạn có chắc chắn muốn xóa bảng "${tableName}"?`)) {
            return;
        }

        const apiUrl = API_BASE_URL + `/excel/delete-table?tableName=${encodeURIComponent(tableName)}`;
        $http
            .delete(apiUrl)
            .then(function (response) {
                alert("Đã xóa bảng thành công.");
                $scope.selectedTable = null;
                $scope.tableData = [];
                $scope.columns = [];
                location.reload();
            })
            .catch(function (error) {
                console.error("Lỗi khi xóa bảng:", error);
                alert("Không thể xóa bảng.");
            });
    };

    // Xử lý combo
    $scope.viewProcessedCombo = function () {
        $scope.flag = true;
        $http
            .get(API_BASE_URL + "/excel/expand-combo")
            .then(function (response) {
                $scope.tableData = response.data;
                $scope.selectedTable = "Kết quả mở rộng Combo";
                if ($scope.tableData.length > 0) {
                    $scope.columns = Object.keys($scope.tableData[0]);
                } else {
                    $scope.columns = [];
                }
            })
            .catch(function (error) {
                console.error("Lỗi khi xử lý combo:", error);
                alert("Không thể xử lý dữ liệu combo.");
            });
    };
    // Lấy giá trị duy nhất của mỗi cột để tạo dropdown
    $scope.getUniqueValues = function (col) {
        const values = $scope.tableData.map(row => row[col]);
        return [...new Set(values)]; // Lọc ra các giá trị duy nhất
    };

    // Hàm lọc tùy chỉnh cho từng cột
    $scope.customFilter = function (row) {
        var match = true;
        angular.forEach($scope.columns, function (col) {
            if ($scope.filters[col] && row[col].toString().indexOf($scope.filters[col]) === -1) {
                match = false;
            }
        });
        return match;
    };


    window.onscroll = function () {
        const btn = document.getElementById("backToTopBtn");
        if (document.body.scrollTop > 200 || document.documentElement.scrollTop > 200) {
            btn.classList.add("active");
        } else {
            btn.classList.remove("active");
        }
    };

// Nếu bạn dùng AngularJS controller
    $scope.scrollToTop = function () {
        window.scrollTo({top: 0, behavior: "smooth"});
    };

});

app.directive("fileModel", [
    "$parse",
    function ($parse) {
        return {
            restrict: "A",
            link: function (scope, element, attrs) {
                const model = $parse(attrs.fileModel);
                const modelSetter = model.assign;

                element.bind("change", function () {
                    scope.$apply(function () {
                        modelSetter(scope, element[0].files[0]);
                    });
                });
            },
        };
    },
]);

app.controller("SingleUploadCtrl", [
    "$scope",
    "$http",
    function ($scope, $http) {
        $scope.uploading = false;
        $scope.uploadSuccess = false;
        $scope.uploadError = false;

        $scope.uploadReport = function (selectedOption) {
            const formData = new FormData();
            formData.append("file", $scope.reportFile);

            // Build URL with query param
            const uploadUrl = "/excel/upload?option=" + encodeURIComponent(selectedOption);

            $scope.uploading = true;
            $scope.uploadSuccess = false;
            $scope.uploadError = false;

            $http
                .post(uploadUrl, formData, {
                    transformRequest: angular.identity,
                    headers: {"Content-Type": undefined},
                })
                .then(
                    function (response) {
                        $scope.result = response.data;
                        $scope.uploading = false;
                        $scope.uploadSuccess = true;

                        // Tự động reload sau vài giây nếu bạn muốn
                        setTimeout(() => {
                            location.reload();
                        }, 1500);
                    },
                    function () {
                        $scope.uploading = false;
                        $scope.uploadError = true;
                    }
                );
        };
    },
]);
