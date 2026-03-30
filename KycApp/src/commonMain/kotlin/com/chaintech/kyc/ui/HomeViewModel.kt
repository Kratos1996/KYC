package com.chaintech.kyc.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaintech.documentcontroller.docdetector.detectorService.DocumentDetectorData
import com.chaintech.documentcontroller.docdetector.detectorService.DocumentDetectorService
import com.chaintech.documentcontroller.documentRectDetector.dataModels.DocumentRectClassName
import com.chaintech.documentcontroller.documentRectDetector.dataModels.DocumentRegionRect
import com.chaintech.documentcontroller.documentRectDetector.dataModels.DocumentValidationResult
import com.chaintech.documentcontroller.documentRectDetector.detectorService.DocumentFieldDetectorService
import com.chaintech.faceantispoofing.dataModels.AntiSpoofingResult
import com.chaintech.faceantispoofing.dataModels.FaceAntiSpoofingInput
import com.chaintech.faceantispoofing.service.FaceAntiSpoofingService
import com.chaintech.facecropper.FaceCropper
import com.chaintech.facematching.dataModels.FaceImageSize
import com.chaintech.facematching.dataModels.FaceMatchInput
import com.chaintech.facematching.dataModels.FaceMatchingResult
import com.chaintech.facematching.service.FaceMatchingService
import com.chaintech.facetoken.service.FaceTokenService
import com.chaintech.googlemlkitocr.TextRecognizer
import com.chaintech.kyc.core.BitmapUtil.isValid
import com.chaintech.kyc.core.navigation.AppScreenKyc
import com.chaintech.kyc.core.navigation.kycNavigation.Navigator
import com.chaintech.kyc.moduleTest.docFieldDetector.toRGBBytes
import com.chaintech.kyc.network.IpInfoState
import com.chaintech.kyc.network.IpRepository
import com.chaintech.kyc.utility.AppStrings
import com.chaintech.kyc.utility.location.LocationRepository
import com.chaintech.kyc.utility.model.IPValues
import com.chaintech.kyc.utility.model.LocationData
import com.chaintech.kyc.utility.model.LocationResult
import com.chaintech.kyc.utility.model.LocationState
import com.chaintech.kyc.utility.model.PublicIp
import com.chaintech.mrzparsersdk.mrzparser.model.MRZResultModel
import com.chaintech.mrzparsersdk.mrzparser.service.MRZExtractionService
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi

data class ScanDocumentOcrModel(
    val ocrText: String,
    val documentRegionRect: DocumentRegionRect,
)

data class ScanDocumentMrzModel(
    val documentRegionRect: ArrayList<DocumentRegionRect>,
    val mrzResultModel: MRZResultModel
)

@OptIn(ExperimentalResourceApi::class)
class HomeViewModel(
    val appString: AppStrings,
    val documentDetectorService: DocumentDetectorService,
    val documentFieldDetectorService: DocumentFieldDetectorService,
    val faceAntiSpoofingService: FaceAntiSpoofingService,
    val faceCropper: FaceCropper,
    val faceMatchingService: FaceMatchingService,
    val ipRepository: IpRepository,
    val textRecognizer: TextRecognizer,
    val mrzParserService: MRZExtractionService,
    val faceTokenService: FaceTokenService,
    val locationRepository: LocationRepository
) : ViewModel() {
    //currentLocation
    private val _locationState = MutableStateFlow(LocationState())
    val locationState = _locationState.asStateFlow()

    // ScannedDocumentData
    private val scannedDocument = ArrayList<ImageBitmap>()
    private val scannedDocumentDetectorData = ArrayList<DocumentDetectorData>()
    private val scannedDocumentFieldDetector = ArrayList<DocumentRegionRect>()
    private val scanDocumentOcrModels = ArrayList<ScanDocumentOcrModel>()
    private val scanDocumentMrzModels = mutableStateOf<ScanDocumentMrzModel?>(null)

    //faceLiveness
    val antiSpoofResult = mutableStateOf<AntiSpoofingResult?>(null)
    val faceMatchingResult = mutableStateOf<FaceMatchingResult?>(null)
    val faceSelfieBitmap = mutableStateOf<ImageBitmap?>(null)
    val faceSelfieCroppedBitmap = mutableStateOf<ImageBitmap?>(null)
    val faceLivenessVideoPath = mutableStateOf<String?>(null)
    val faceLivenessImagePath = mutableStateOf<String?>(null)
    val faceToken = mutableStateOf<FloatArray?>(null)
    //network

    private val _state = MutableStateFlow(IpInfoState())
    val state = _state.asStateFlow()
    val ipData = mutableStateOf<PublicIp.IpInfo?>(null)

    //Navigation
    private val _navListener = Channel<Navigator>()
    val navListener = _navListener.receiveAsFlow()


    val isProgressLoader = mutableStateOf(true)
    val isError = mutableStateOf(false)
    val errorMessage = mutableStateOf("")
    val isSuccess = mutableStateOf(false)

    val isFaceDataAvailableOnServer = mutableStateOf(false)

    init {
        viewModelScope.launch {
           getIpv6Data()
        }
    }

    //DocumentScanner
    val loadDocumnetScanner = mutableStateOf(DocumentScanner())

    fun documentScanningComplete(documents: List<ImageBitmap?>?) {
        scannedDocument.clear()
        if (!documents.isNullOrEmpty()) {
            scannedDocument.addAll(documents.filterNotNull())
        }
        navigate(Navigator.NavigateRoute(AppScreenKyc.VerifyDataScreen))
    }

    suspend fun startDocumentCropping() {
        try {
            showLoader()
            scannedDocumentDetectorData.clear()
            scannedDocumentFieldDetector.clear()

            if (scannedDocument.isEmpty()) {
                showError(appString.documentNotAvailable)
                return // Early return
            }

            documentDetectorService.initialize()
            scannedDocument.fastForEach { images ->
                images.let { finalImage ->
                    val croppedDocument = detectAndCropDocument(finalImage)
                    scannedDocumentDetectorData.add(croppedDocument)
                }
            }
            documentDetectorService.dispose()

            extractFieldsFromDocument()

            // OCR Processing
            val result = validateDocumentFields(scannedDocumentFieldDetector)
            when (result) {
                DocumentValidationResult.ValidWithTesseract -> {
                    performMRZParserOcr()
                }
                DocumentValidationResult.ValidWithOCR -> {
                    performOcr()
                }
                DocumentValidationResult.Failed -> {
                    showError("Document not valid")
                    return
                }
            }

            // Face matching only happens if validation passed
            faceMatchingProcess()
        } catch (e: Exception) {
            val errorMessage = e.message ?: "We are facing some issue in your document. Please try again!"
            showError(errorMessage)
        }
    }

    private suspend fun detectAndCropDocument(imageBitmap: ImageBitmap): DocumentDetectorData {
        val documentDetectorData = documentDetectorService.detectDocument(
            image = imageBitmap,
            imageWidth = imageBitmap.width,
            imageHeight = imageBitmap.height
        )
        return documentDetectorData
    }

    private suspend fun extractFieldsFromDocument() {
        if (scannedDocumentDetectorData.isEmpty()) {
            showError(appString.documentNotAvailable)
            return
        }

        documentFieldDetectorService.initialize()
        try {
            val allRectForScanDocument = ArrayList<DocumentRegionRect>()

            scannedDocumentDetectorData.fastForEachIndexed { _, croppedBitmaps ->
                try {
                    val result = documentFieldDetectorService.detectFields(
                        image = croppedBitmaps.croppedBitmap,
                        imageWidth = croppedBitmaps.croppedBitmap.width,
                        imageHeight = croppedBitmaps.croppedBitmap.height
                    )
                    allRectForScanDocument.addAll(result)
                    println("DocumentFieldDetector: ScanDocument - $result")
                } catch (e: Exception) {
                    e.printStackTrace()
                    throw e
                }
            }

            val allRectWithHighConfidence = getHighestConfidenceObjects(allRectForScanDocument)

            if (allRectWithHighConfidence.isEmpty()) {
                showError(appString.documentNotAvailable)
                return
            }
            println("DocumentFieldDetector: allRectWithHighConfidence - $allRectWithHighConfidence")
            scannedDocumentFieldDetector.addAll(allRectWithHighConfidence)
        } finally {
            // Always dispose, even if error occurs
            documentFieldDetectorService.dispose()
        }
    }

    /**
     * Filters the OCR results and returns only the objects with the highest confidence score.
     *
     * @param ocrResult A list of [DocumentRegionRect] containing detected objects with confidence values.
     * @return A list of [DocumentRegionRect] containing only the highest-confidence objects.
     */
    private fun getHighestConfidenceObjects(regions: List<DocumentRegionRect>): List<DocumentRegionRect> {
        val bestByClassId = mutableMapOf<Int, DocumentRegionRect>()

        for (region in regions) {
            val classId = region.classId
            val confidence = region.confidence

            val existing = bestByClassId[classId]
            if (existing == null || confidence > existing.confidence) {
                bestByClassId[classId] = region
            }
        }

        return bestByClassId.values.sortedBy { it.classId }
    }

    /**
     * Get ocr text from detect field from DocumentFieldDetector object
     */
    private suspend fun performOcr() {
        try {
            scannedDocumentFieldDetector.fastForEachIndexed { index, it ->
                val image = it.image
                val ocrString = textRecognizer.processImage(imageData = image)
                scanDocumentOcrModels.add(
                    ScanDocumentOcrModel(
                        ocrText = ocrString,
                        documentRegionRect = it
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        } finally {
            textRecognizer.release()
        }
    }

    /**
     * Get MRZ data class with full detail from MRZ image
     */
    private suspend fun performMRZParserOcr() {
        try {
            val mrzImage = getCroppedImagesFromDocument(docName = DocumentRectClassName.MRZ.value)
            mrzImage?.let {
                mrzParserService.initialize()
                val mrzResultModel = mrzParserService.parseMrz(mrzImage)
                println("HomeViewModel: PerformMRZParserOcr -> $mrzResultModel")
                mrzResultModel?.let {
                    scanDocumentMrzModels.value =
                        ScanDocumentMrzModel(scannedDocumentFieldDetector, mrzResultModel)
                }
                mrzParserService.dispose()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    private suspend fun faceMatchingProcess() {
        isProgressLoader.value = true
        //FaceMatching
        val getPhotoReactsForCrop = getCroppedImagesFromDocument(DocumentRectClassName.PHOTO.value)
        if (scannedDocument.isNotEmpty()) {
            faceMatchingService.initialize()
            getPhotoReactsForCrop?.let { documnetImage ->
                // Detect Face Matching Result
                faceSelfieCroppedBitmap.value?.let { selfie ->
                    faceMatchingResult.value = faceMatchingService.isSamePerson(
                        input = FaceMatchInput(
                            firstImage = documnetImage.toRGBBytes(),
                            secondImage = selfie.toRGBBytes(),
                            firstFaceImageSize = FaceImageSize(
                                documnetImage.width,
                                documnetImage.height
                            ),
                            secondFaceImageSize = FaceImageSize(
                                selfie.width,
                                selfie.height
                            ),
                            threshold = 0.2f
                        )
                    )
                    if (faceMatchingResult.value?.isSimilar == true) {
                        isProgressLoader.value = false
                        isError.value = false
                        isSuccess.value = true
                        errorMessage.value = ""
                    } else {
                        isProgressLoader.value = false
                        isError.value = true
                        isSuccess.value = false
                        errorMessage.value = appString.faceNotMatched
                    }

                    println("FaceMatching : result: $faceMatchingResult")
                }
            }
        } else {
            isProgressLoader.value = false
            isError.value = true
            isSuccess.value = false
            errorMessage.value = appString.documentNotAvailable
        }
    }

    fun validateDocumentFields(scannedFields: List<DocumentRegionRect>): DocumentValidationResult {
        // Extract all detected class names
        val detectedClasses = scannedFields.map { it.className }.toSet()

        // Case 1: MRZ + PHOTO → use Tesseract
        if (detectedClasses.containsAll(
                listOf(
                    DocumentRectClassName.MRZ,
                    DocumentRectClassName.PHOTO
                )
            )
        ) {
            println("ValidateDocumentFields - ValidWithTesseract: Case 1")
            return DocumentValidationResult.ValidWithTesseract
        }

        // Case 2: PHOTO + FULL_NAME + ID_NUMBER
        if (detectedClasses.containsAll(
                listOf(
                    DocumentRectClassName.PHOTO,
                    DocumentRectClassName.FULL_NAME,
                    DocumentRectClassName.ID_NUMBER
                )
            )
        ) {
            println("ValidateDocumentFields - ValidWithOCR: Case 2")
            return DocumentValidationResult.ValidWithOCR
        }

        // Case 3: PHOTO + GIVEN_NAME + SURNAME + ID_NUMBER
        if (detectedClasses.containsAll(
                listOf(
                    DocumentRectClassName.PHOTO,
                    DocumentRectClassName.GIVEN_NAME,
                    DocumentRectClassName.SURNAME,
                    DocumentRectClassName.ID_NUMBER
                )
            )
        ) {
            println("ValidateDocumentFields - ValidWithOCR: Case 3")
            return DocumentValidationResult.ValidWithOCR
        }

        println("ValidateDocumentFields - Failed: Default")
        // Default: validation failed
        return DocumentValidationResult.Failed
    }

    fun getScannedDocument() = scannedDocumentDetectorData

    fun permissionDeniedView() {

    }

    fun getCroppedImagesFromDocument(docName: String): ImageBitmap? {
        return scannedDocumentFieldDetector.find { it.className.value == docName }?.image
    }

    // faceLiveness
    fun faceLivenessComplete(faceBitmap: ImageBitmap, videoPath: String, imagePath: String) {
        faceSelfieBitmap.value = faceBitmap
        faceLivenessVideoPath.value = videoPath
        faceLivenessImagePath.value = imagePath
        isProgressLoader.value = true
        isSuccess.value = false
        isError.value = false
        navigate(Navigator.NavigateRoute(AppScreenKyc.VerifyingFaceDataScreen))
    }

    suspend fun startWorkOnFaceData() {
        showLoader()
        faceAntiSpoofingService.initialize()
        if (faceSelfieBitmap.value.isValid()) {
            try {
                faceSelfieBitmap.value?.let { finalBitmap ->
                    faceCropper.getCroppedFaceUsingBitmap(image = finalBitmap, expandRatio = 1.2f)
                        .fold(
                            onSuccess = { result ->
                                faceSelfieCroppedBitmap.value = result.bitmap
                                faceSelfieCroppedBitmap.value?.let { finalCroppedImageBytes ->
                                    antiSpoofResult.value =
                                        faceAntiSpoofingService.isSpoofedFaceDetected(
                                            input = FaceAntiSpoofingInput(
                                                image = finalCroppedImageBytes.toRGBBytes(),
                                                width = finalCroppedImageBytes.width,
                                                height = finalCroppedImageBytes.height
                                            )
                                        )
                                    if (antiSpoofResult.value?.isSpoofed == true) {
                                        hideLoader()
                                        isError.value = true
                                        isSuccess.value = false
                                        errorMessage.value = appString.anti_spoofing_message
                                    } else {
                                        getFaceToken(finalCroppedImageBytes)
                                        hideLoader()
                                        isError.value = false
                                        isSuccess.value = true
                                        errorMessage.value = ""
                                    }
                                    faceAntiSpoofingService.dispose()
                                }
                                faceCropper.close()
                            },
                            onFailure = { exception ->
                                hideLoader()
                                isError.value = true
                                isSuccess.value = false
                                errorMessage.value = exception.message
                                    ?: "Currently we are facing some issues in your face data."
                                faceCropper.close()
                            }
                        )
                    faceCropper.close()
                }
            } catch (e: Exception) {
                hideLoader()
                isError.value = true
                isSuccess.value = false
                errorMessage.value =
                    e.message ?: "Currently we are facing some issues in your face data."
            }
        } else {
            hideLoader()
            isError.value = true
            isSuccess.value = false
            errorMessage.value = "Currently we are facing some issues in your face data."
        }
    }

    private suspend fun getFaceToken(faceImageBitmap: ImageBitmap) {
        try {
            faceTokenService.initialize()
            faceToken.value = faceTokenService.getFaceToken(faceImageBitmap)
            println("FaceToken Value:- ${faceToken.value?.contentToString()}")
        } catch (e: Exception) {
            hideLoader()
            isError.value = true
            isSuccess.value = false
            errorMessage.value =
                e.message ?: "Currently we are facing some issues in your face data."
        }
    }

    //Network
    // Initialize and get IPv6 data
    fun getIpv6Data() {
        viewModelScope.launch {
            try {
                // Get local IPv6 addresses
                val ipv6s = ipRepository.getLocalIPv6Addresses()
                val ipv6List = ArrayList<String>()
                println("getIpv6Data: $ipv6s")
                ipv6s.forEach {
                    if (!it.startsWith("fe80")) {
                        ipv6List.add(it)
                    }
                }

                if (ipv6List.isNotEmpty()) {
                    IPValues.ipv6 = ipv6List.first()
                }

                // If no IPv6, get public IP
                if (IPValues.ipv6.isNullOrEmpty()) {
                    ipRepository.getPublicIp()
                        .onSuccess { publicIp ->
                            IPValues.publicIp = publicIp
                            fetchIpInfo(publicIp)
                            println("IPValues.ipInfo: ${IPValues.ipInfo}")
                        }
                        .onFailure { exception ->
                            println("IPValues.ipInfo: Failed to get public IP: ${exception.message}")
                            _state.update {
                                it.copy(error = "IPValues.ipInfo: Failed to get public IP: ${exception.message}")
                            }
                        }
                } else {
                    // Use IPv6
                    fetchIpInfo(IPValues.ipv6.toString())
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                _state.update {
                    it.copy(error = "IPValues.ipInfo: Error getting IPv6 data: ${ex.message}")
                }
            }
        }
    }

    // Fetch IP information
    fun fetchIpInfo(ip: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            ipRepository.getIpInfo(ip)
                .onSuccess { ipInfo ->
                    ipData.value = ipInfo
                    IPValues.ipInfo = ipInfo
                    println("FetchIpInfo: IPValues.ipInfo - ${IPValues.ipInfo}")
                    _state.update {
                        it.copy(
                            ipInfo = ipInfo,
                            isLoading = false,
                            error = null
                        )
                    }
                }
                .onFailure { exception ->
                    println("FetchIpInfo: exception.message - ${exception.message}")
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "Unknown error"
                        )
                    }
                }
        }
    }

    //Location
    fun fetchCurrentLocation(
        onSuccess: suspend (location: LocationData) -> Unit,
        onError:suspend (LocationResult)-> Unit
        ) {
        isProgressLoader.value = true
        viewModelScope.launch {
            _locationState.update { it.copy(isLoading = true, error = null) }

            when (val result = locationRepository.getCurrentLocation()) {
                is LocationResult.Success -> {
                    _locationState.update {
                        it.copy(
                            location = result.location,
                            isLoading = false,
                            permissionDenied = false,
                            locationDisabled = false
                        )
                    }
                    _locationState.value.location?.let { onSuccess( it) }
                }

                is LocationResult.Error -> {
                    onError(LocationResult.Error(result.message,isError = true))
                    _locationState.update {
                        it.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }

                LocationResult.PermissionDenied -> {
                    onError(LocationResult.PermissionDenied)
                    _locationState.update {
                        it.copy(
                            isLoading = false,
                            permissionDenied = true
                        )
                    }
                }

                LocationResult.LocationDisabled -> {
                    onError(LocationResult.LocationDisabled)
                    _locationState.update {
                        it.copy(
                            isLoading = false,
                            locationDisabled = true
                        )
                    }
                }
            }
        }
    }

    fun openLocationSettings() {
        locationRepository.openSettings()
    }


    // Clear error message
    fun clearError() {
        _state.update { it.copy(error = null) }
        _locationState.update { it.copy(error = null) }
    }


    //Navigation
    fun navigate(navigator: Navigator) {
        viewModelScope.launch {
            _navListener.send(navigator)
        }
    }

    fun showLoader() {
        isProgressLoader.value = true
    }

    fun hideLoader() {
        isProgressLoader.value = false
    }

    fun showError(error: String) {
        hideLoader()
        isError.value = true
        isSuccess.value = false
        errorMessage.value = error
    }

    fun resetAllData() {
        scannedDocument.clear()
        scannedDocumentDetectorData.clear()
        faceMatchingResult.value = null
        scannedDocumentFieldDetector.clear()
        scanDocumentOcrModels.clear()
        scanDocumentMrzModels.value = null
        faceAntiSpoofingService.dispose()
        clearError()
    }

    fun resetDataForReKyc() {
        scannedDocument.clear()
        scannedDocumentDetectorData.clear()
        faceMatchingResult.value = null
        scannedDocumentFieldDetector.clear()
        scanDocumentOcrModels.clear()
        scanDocumentMrzModels.value = null
    }

    fun navigateBack() {
        navigate(Navigator.NavigateBack)
    }

    /**
     * Submit screen data show methods
     */
    fun showScanDocumentDetails(): List<Pair<String, String>> {
        val mrzResult = scanDocumentMrzModels.value?.mrzResultModel

        return if (mrzResult != null) {
            getDocumentMrzDetails(mrzResult)
        } else {
            getDocumentOcrDetails()
        }
    }

    private fun getDocumentOcrDetails(): List<Pair<String, String>> {
        val rows = mutableListOf<Pair<String, String>>()

        for (item in scanDocumentOcrModels) {
            val field = item.documentRegionRect.className
            val text = item.ocrText

            when (field) {
                DocumentRectClassName.ID_NUMBER -> {
                    rows.add("Document Number" to text)
                }

                DocumentRectClassName.FULL_NAME -> {
                    rows.add("Full Name" to text)
                }

                DocumentRectClassName.SEX -> {
                    rows.add("Sex" to text)
                }

                DocumentRectClassName.DATE_OF_BIRTH,
                DocumentRectClassName.DOCUMENT_ISSUE_DATE,
                DocumentRectClassName.ISSUING_COUNTRY,
                DocumentRectClassName.ADDRESS -> {
                    if (text.isNotEmpty()) {
                        val title = when (field) {
                            DocumentRectClassName.DATE_OF_BIRTH -> "Birthdate"
                            DocumentRectClassName.DOCUMENT_ISSUE_DATE -> "Document Issue Date"
                            DocumentRectClassName.ISSUING_COUNTRY -> "Document Issue Country"
                            DocumentRectClassName.ADDRESS -> "Address"
                            else -> ""
                        }
                        rows.add(title to text)
                    }
                }

                else -> Unit
            }
        }

        return rows
    }

    private fun getDocumentMrzDetails(mrzScanResult: MRZResultModel): List<Pair<String, String>> {
        val rows = mutableListOf<Pair<String, String>>()

        // Required fields
        rows.add("MRZ Type" to mrzScanResult.mrzType)
        rows.add("Surnames" to mrzScanResult.surnames)
        rows.add("Given Names" to mrzScanResult.givenNames)
        rows.add("Document Type" to mrzScanResult.documentType)
        rows.add("Document Number" to mrzScanResult.documentNumber)
        rows.add("Country Code" to mrzScanResult.countryCode)
        rows.add("Nationality Country Code" to mrzScanResult.nationalityCountryCode)

        // Optional fields — only add if non-empty
        val optionalFields: List<Pair<String, String?>> = listOf(
            "Birthdate" to mrzScanResult.birthdate,
            "Sex" to mrzScanResult.sex,
            "Expiry Date" to mrzScanResult.expiryDate,
            "Optional Data" to mrzScanResult.optionalData,
            "Optional Data2" to mrzScanResult.optionalData2
        )

        optionalFields.forEach { (title, value) ->
            if (!value.isNullOrBlank()) {
                rows.add(title to value)
            }
        }

        return rows
    }

    /********************************************************************************/

    data class DocumentScanner(
        val modifier: Modifier = Modifier.Companion,
        val overlayColor: Color = Color(0x88000000),
        val overlayBorderColor: Color = Color(0xFF4A4A49),
        val overlayBorderRoundedCornerColor: Color = Color.Companion.White,
        val noOfDocuments: Int = 1,
        val documentType: String = "ID",
    )
}
