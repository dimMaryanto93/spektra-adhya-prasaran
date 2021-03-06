package app.controller.penggajian;

import java.io.IOException;
import java.net.URL;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ResourceBundle;

import org.controlsfx.control.Notifications;
import org.controlsfx.dialog.ExceptionDialog;
import org.controlsfx.dialog.ProgressDialog;
import org.controlsfx.validation.Severity;
import org.controlsfx.validation.ValidationResult;
import org.controlsfx.validation.ValidationSupport;
import org.controlsfx.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import app.configs.BootFormInitializable;
import app.configs.PrintConfig;
import app.configs.StringFormatterFactory;
import app.controller.HomeController;
import app.entities.kepegawaian.KehadiranKaryawan;
import app.entities.kepegawaian.Penggajian;
import app.entities.kepegawaian.uang.prestasi.Motor;
import app.entities.kepegawaian.uang.prestasi.PembayaranCicilanMotor;
import app.entities.master.DataJabatan;
import app.entities.master.DataKaryawan;
import app.repositories.RepositoryAbsensi;
import app.repositories.RepositoryCicilanMotor;
import app.repositories.RepositoryKaryawan;
import app.repositories.RepositoryPenggajianKaryawan;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Duration;
import net.sf.jasperreports.engine.JRException;

@Component
public class PenggajianKaryawanPencairanDanaController implements BootFormInitializable {

    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private ApplicationContext springContext;

    @FXML
    private Button btnSave;
    @FXML
    private ComboBox<String> txtNip;
    @FXML
    private TextField txtNama;
    @FXML
    private TextField txtJabatan;
    @FXML
    private TextField txtJenisKelamin;
    @FXML
    private TextField txtGajiPokok;
    @FXML
    private TextField txtTotalKehadiran;
    @FXML
    private TextField txtJumlahKehadiran;
    @FXML
    private TextField txtJumlahLembur;
    @FXML
    private TextField txtTotalLembur;
    @FXML
    private TextField txtCicilanKe;
    @FXML
    private TextField txtMerekMotor;
    @FXML
    private TextField txtUangPrestasi;
    @FXML
    private TextField txtTotal;
    @FXML
    private TextField txtPotonganGajiPokok;
    @FXML
    private TextField txtKelebihanGaji;
    @FXML
    private CheckBox checkValid;

    @Autowired
    private RepositoryPenggajianKaryawan servicePenggajian;
    @Autowired
    private RepositoryKaryawan serviceKaryawan;
    @Autowired
    private RepositoryCicilanMotor serviceCicilanMotor;
    @Autowired
    private StringFormatterFactory stringFormatter;
    @Autowired
    private RepositoryAbsensi serviceAbsen;
    @Autowired
    private PrintConfig print;
    @Autowired
    private HomeController homeController;

    private HashMap<String, DataKaryawan> mapKaryawan;
    private Penggajian penggajian;
    private List<KehadiranKaryawan> listTransport = new ArrayList<KehadiranKaryawan>();
    private List<KehadiranKaryawan> listLembur = new ArrayList<KehadiranKaryawan>();

    private PembayaranCicilanMotor pembayaranCicilanMotor;
    private Motor cicilanMotor;

    private ValidationSupport validation;

    private void clearFields() {
        txtNip.getSelectionModel().clearSelection();
        txtNama.clear();
        txtJabatan.clear();
        txtJenisKelamin.clear();
        txtGajiPokok.clear();
        txtTotalKehadiran.clear();
        txtJumlahKehadiran.clear();
        txtJumlahLembur.clear();
        txtTotalLembur.clear();
        txtCicilanKe.clear();
        txtMerekMotor.clear();
        txtUangPrestasi.clear();
        txtPotonganGajiPokok.clear();
        txtTotal.clear();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        checkValid.setDisable(true);
        checkValid.setOpacity(0D);

        txtNip.setCellFactory(new Callback<ListView<String>, ListCell<String>>() {

            @Override
            public ListCell<String> call(ListView<String> param) {
                return new ListCell<String>() {

                    private DataKaryawan karyawan;

                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setText(null);
                        } else {
                            this.karyawan = mapKaryawan.get(item);
                            StringBuilder sb = new StringBuilder(karyawan.getNip()).append(" (")
                                    .append(karyawan.getNama()).append(" bagian ")
                                    .append(karyawan.getJabatan().getNama()).append(")");
                            setText(sb.toString());
                        }
                    }
                };
            }
        });
        txtNip.getSelectionModel().selectedItemProperty().addListener((s, old, value) -> {
            this.checkValid.setDisable(value == null);
            this.checkValid.setSelected(false);
            this.checkValid.setText("");
            if (value != null) {
                this.checkValid.setOpacity(1D);
                this.checkValid.setText("Saya setuju");
                setFields(mapKaryawan.get(value));
            } else {
                checkValid.setOpacity(0D);
                clearFields();
            }
        });
        initValidator();

    }

    private void setFields(DataKaryawan karyawan) {
        this.penggajian.setKaryawan(karyawan);

        LocalDate sekarang = LocalDate.now();
        LocalDate awalBulan = sekarang.withDayOfMonth(1);
        LocalDate akhirBulan = sekarang.withDayOfMonth(sekarang.lengthOfMonth());

        this.penggajian.setTahunBulan(stringFormatter.getDateIndonesionFormatterOnlyYearAndMonth(sekarang));

        DataJabatan jabatan = karyawan.getJabatan();

        txtNama.setText(karyawan.getNama());
        txtJabatan.setText(jabatan.getNama());
        txtJenisKelamin.setText(karyawan.getJenisKelamin().toString());

        this.penggajian.setGajiPokok(karyawan.getGajiPokok());
        txtGajiPokok.setText(stringFormatter.getCurrencyFormate(karyawan.getGajiPokok()));

        try {

            this.listTransport.clear();
            listTransport.addAll(serviceAbsen.findByKaryawanAndTanggalHadirBetweenAndHadir(karyawan,
                    Date.valueOf(awalBulan), Date.valueOf(akhirBulan), true));

            this.penggajian.setJumlahKehadiran(listTransport.size());
            txtJumlahKehadiran
                    .setText(stringFormatter.getNumberIntegerOnlyFormate(this.penggajian.getJumlahKehadiran()));
            this.penggajian.setUangTransport(this.penggajian.getJumlahKehadiran() * 30000D);

            txtTotalKehadiran.setText(stringFormatter.getCurrencyFormate(this.penggajian.getUangTransport()));
        } catch (Exception e) {
            logger.error("Tidak dapat mendapatkan data absensi karyawan atas nama {}", karyawan.getNama(), e);

            StringBuilder errorMessage = new StringBuilder(
                    "Tidak dapat menghitung jumlah kehadiran karyawan atas nama ");
            errorMessage.append(karyawan.getNama()).append(" dengan NIP ").append(karyawan.getNip());
            ExceptionDialog ex = new ExceptionDialog(e);
            ex.setTitle("Pencairan penggajian karyawan");
            ex.setHeaderText(errorMessage.toString());
            ex.setContentText(e.getMessage());
            ex.initModality(Modality.APPLICATION_MODAL);
            ex.show();
        }

        try {
            this.listLembur.clear();
            this.listLembur.addAll(serviceAbsen.findByKaryawanAndTanggalHadirBetweenAndLembur(karyawan,
                    Date.valueOf(awalBulan), Date.valueOf(akhirBulan), true));

            this.penggajian.setJumlahLembur(listLembur.size());
            txtJumlahLembur.setText(stringFormatter.getNumberIntegerOnlyFormate(this.penggajian.getJumlahLembur()));
            this.penggajian.setUangLembur(this.penggajian.getJumlahLembur() * 30000D);
            txtTotalLembur.setText(stringFormatter.getCurrencyFormate(this.penggajian.getUangLembur()));
        } catch (Exception e) {
            logger.error("Tidak dapat mendapatkan data lembur karyawan atas nama {}", karyawan.getNama(), e);

            StringBuilder errorMessage = new StringBuilder("Tidak dapat menghitung jumlah lembur karyawan atas nama ");
            errorMessage.append(karyawan.getNama()).append(" dengan NIP ").append(karyawan.getNip());
            ExceptionDialog ex = new ExceptionDialog(e);
            ex.setTitle("Pencairan penggajian karyawan");
            ex.setHeaderText(errorMessage.toString());
            ex.setContentText(e.getMessage());
            ex.initModality(Modality.APPLICATION_MODAL);
            ex.show();
        }

        this.cicilanMotor = karyawan.getNgicilMotor();
        Double bayarCicilanMotor;
        if (cicilanMotor != null && cicilanMotor.isSetuju()) {
            this.pembayaranCicilanMotor = new PembayaranCicilanMotor();
            this.pembayaranCicilanMotor.setTanggalBayar(Date.valueOf(LocalDate.now()));
            this.pembayaranCicilanMotor.setAngsuranKe(serviceCicilanMotor.findByMotor(cicilanMotor).size() + 1);
            Double cicilan = 500000D;
            Double kelebihanCicilan = cicilanMotor.getPembayaran() - cicilan;

            if (kelebihanCicilan > 0) {
                this.penggajian.setGajiPokok(karyawan.getGajiPokok() + kelebihanCicilan);
                txtPotonganGajiPokok.setText(stringFormatter.getCurrencyFormate(-kelebihanCicilan));
                txtKelebihanGaji.setText(stringFormatter.getCurrencyFormate(0));
            } else if (kelebihanCicilan == 0) {
                txtKelebihanGaji.setText(stringFormatter.getCurrencyFormate(0));
                txtPotonganGajiPokok.setText(stringFormatter.getCurrencyFormate(0));
                this.penggajian.setGajiPokok(karyawan.getGajiPokok() - kelebihanCicilan);
            } else {
                txtPotonganGajiPokok.setText(stringFormatter.getCurrencyFormate(0));
                this.penggajian.setGajiPokok(karyawan.getGajiPokok() - kelebihanCicilan);
                txtKelebihanGaji.setText(stringFormatter.getCurrencyFormate(-kelebihanCicilan));
            }
            this.pembayaranCicilanMotor.setBayar(cicilanMotor.getPembayaran());
            this.pembayaranCicilanMotor.setMotor(cicilanMotor);
            bayarCicilanMotor = this.pembayaranCicilanMotor.getBayar();

            txtGajiPokok.setText(stringFormatter.getCurrencyFormate(karyawan.getGajiPokok()));
            txtCicilanKe.setText(
                    stringFormatter.getNumberIntegerOnlyFormate(this.pembayaranCicilanMotor.getAngsuranKe()) + "x");
            txtMerekMotor.setText(cicilanMotor.getMerkMotor());
            txtUangPrestasi.setText(stringFormatter.getCurrencyFormate(this.pembayaranCicilanMotor.getBayar()));
        } else {
            bayarCicilanMotor = 0D;

            txtPotonganGajiPokok.clear();
            txtCicilanKe.clear();
            txtMerekMotor.clear();
            txtUangPrestasi.clear();
        }

        Double totalGaji = this.penggajian.getGajiPokok() + this.penggajian.getUangLembur()
                + this.penggajian.getUangTransport() + bayarCicilanMotor;

        this.txtTotal.setText(stringFormatter.getCurrencyFormate(totalGaji));
    }

    @Override
    public Node initView() throws IOException {
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getResource("/scenes/inner/penggajian/PencairanDana.fxml"));
        loader.setController(springContext.getBean(this.getClass()));
        return loader.load();
    }

    @Override
    public void setStage(Stage stage) {

    }

    @Override
    public void initConstuct() {
        try {
            this.penggajian = new Penggajian();
            this.penggajian.setTanggal(Date.valueOf(LocalDate.now()));

            this.mapKaryawan = new HashMap<String, DataKaryawan>();
            txtNip.getItems().clear();
            List<DataKaryawan> daftarKaryawan = serviceKaryawan.findAll();
            for (DataKaryawan karyawan : daftarKaryawan) {
                Penggajian gaji = servicePenggajian.findByKaryawanAndTahunBulan(karyawan,
                        stringFormatter.getDateIndonesionFormatterOnlyYearAndMonth(LocalDate.now()));
                if (gaji == null && karyawan.isAktifBekerja()) {
                    this.mapKaryawan.put(karyawan.getNip(), karyawan);
                    this.txtNip.getItems().add(karyawan.getNip());
                }
            }
        } catch (Exception e) {
            logger.error("Tidak dapat mendapatkan data karyawan yang belum menerima gaji pada bulan {}",
                    stringFormatter.getDateIndonesionFormatterOnlyYearAndMonth(LocalDate.now()));

            StringBuilder errorMessage = new StringBuilder(
                    "Tidak dapat mendapatkan data karyawan yang belum menerima gaji pada bulan ");
            errorMessage.append(stringFormatter.getDateIndonesionFormatterOnlyYearAndMonth(LocalDate.now()));

            ExceptionDialog ex = new ExceptionDialog(e);
            ex.setTitle("Pencairan penggajian karyawan");
            ex.setHeaderText(errorMessage.toString());
            ex.setContentText(e.getMessage());
            ex.initModality(Modality.APPLICATION_MODAL);
            ex.show();
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.springContext = applicationContext;
    }

    @Override
    public void setMessageSource(MessageSource messageSource) {

    }

    @Override
    public void initValidator() {
        this.validation = new ValidationSupport();
        this.validation.registerValidator(txtNip, true,
                Validator.createEmptyValidator("Karyawan belum dipilih!", Severity.ERROR));
        this.validation.registerValidator(checkValid, (Control c, Boolean value) -> ValidationResult.fromErrorIf(c,
                "Anda belum mengetujui perjanjian!", !value));

        this.validation.invalidProperty().addListener((b, old, value) -> {
            btnSave.setDisable(value);
        });
    }

    @FXML
    public void doSave(ActionEvent event) {
        try {
            DataKaryawan karyawan = penggajian.getKaryawan();
            Task<Object> task = new Task<Object>() {

                @Override
                protected Object call() throws Exception {
                    for (int i = 0; i < 100; i++) {
                        Thread.sleep(10);
                        updateProgress(i, 99);
                        updateMessage("Menyiapkan data untuk menyimpan data penggajian...");
                    }
                    servicePenggajian.save(penggajian);

                    if (cicilanMotor != null) {
                        for (int i = 0; i < 100; i++) {
                            Thread.sleep(10);
                            updateProgress(i, 99);
                            updateMessage("Menyiapkan data untuk menyimpan data cicilan angsuran motor...");
                        }

                        serviceCicilanMotor.save(pembayaranCicilanMotor);
                    }

                    for (int i = 0; i < 100; i++) {
                        Thread.sleep(10);
                        updateProgress(i, 99);
                        updateMessage("Menyiapkan data untuk mencetak kwitansi penggajian...");
                    }
                    printed(penggajian, pembayaranCicilanMotor);

                    succeeded();
                    return null;
                }

                @Override
                protected void succeeded() {
                    try {
                        for (int i = 0; i < 100; i++) {
                            Thread.sleep(10);
                            updateProgress(i, 99);
                            updateMessage("Menyelesaikan proses...");
                        }
                        super.succeeded();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            };

            task.setOnSucceeded(new EventHandler<WorkerStateEvent>() {

                @Override
                public void handle(WorkerStateEvent event) {

                    initConstuct();
                    Notifications.create().title("Pencairan gaji karyawan")
                            .text("Gaji karyawan berhasil disimpan dan dicetak!").hideAfter(Duration.seconds(4D))
                            .hideCloseButton().position(Pos.BOTTOM_RIGHT).showInformation();
                }
            });

            ProgressDialog progres = new ProgressDialog(task);
            progres.setTitle("Penggajian karyawan");
            progres.setHeaderText("Proses pencairan dana penggajian karyawan atas nama " + karyawan.getNama());
            progres.initModality(Modality.APPLICATION_MODAL);
            progres.show();
            Thread th = new Thread(task);
            th.setDaemon(true);
            th.start();

        } catch (Exception e) {
            logger.error("Tidak dapat menyimpan penggajian karyawan atas nama {} sebesar {}",
                    this.penggajian.getKaryawan().getNama(), txtTotal.getText());

            StringBuilder errorMessage = new StringBuilder("Tidak dapat mengimpan penggajian karyawan atas nama ");
            errorMessage.append(this.penggajian.getKaryawan().getNama());
            errorMessage.append(" dengan NIP ").append(this.penggajian.getKaryawan().getNip());
            errorMessage.append(" pada bulan ")
                    .append(stringFormatter.getDateIndonesionFormatterOnlyYearAndMonth(LocalDate.now()));
            ExceptionDialog ex = new ExceptionDialog(e);
            ex.setTitle("Pencairan penggajian karyawan");
            ex.setHeaderText(errorMessage.toString());
            ex.setContentText(e.getMessage());
            ex.initModality(Modality.APPLICATION_MODAL);
            ex.show();
        }
    }

    private void printed(Penggajian gaji, PembayaranCicilanMotor cicilan) {

        try {
            HashMap<String, Object> map = new HashMap<String, Object>();
            DataKaryawan karyawan = gaji.getKaryawan();
            map.put("nip", karyawan.getNip());
            map.put("nama", karyawan.getNama());
            map.put("bulan", penggajian.getTahunBulan());
            map.put("gapok", penggajian.getGajiPokok());
            map.put("transport", penggajian.getUangTransport());
            map.put("lembur", penggajian.getUangLembur());
            map.put("prestasi", 0D);
            if (cicilan != null) {
                map.put("prestasi", cicilan.getBayar());
            }

            print.setValue("/jasper/penggajian/SlipGajiKaryawan.jasper", map);
            print.doPrinted();
        } catch (JRException e) {
            logger.error("Tidak dapat print dokument", e);

            ExceptionDialog ex = new ExceptionDialog(e);
            ex.setTitle("Pencairan penggajian karyawan");
            ex.setHeaderText("Tidak dapat mencetak dokument penggajian karyawan atas nama "
                    + gaji.getKaryawan().getNama() + " dengan NIP " + gaji.getKaryawan().getNip());
            ex.setContentText(e.getMessage());
            ex.initModality(Modality.APPLICATION_MODAL);
            ex.show();
        }
    }

    @FXML
    public void doBack(ActionEvent event) {
        homeController.showWellcome();
    }

    @Override
    public void initIcons() {
        // TODO Auto-generated method stub

    }

}
