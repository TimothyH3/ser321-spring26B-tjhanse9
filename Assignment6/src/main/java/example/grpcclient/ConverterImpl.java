package example.grpcclient;
import java.util.ArrayList;
import java.util.List;

import io.grpc.stub.StreamObserver;
import service.ConversionRequest;
import service.ConversionResponse;
import service.ConverterGrpc;

class ConverterImpl extends ConverterGrpc.ConverterImplBase{
    // Lists to hold conversion types
    private List<String> temperatureTypes = new ArrayList<>();
    private List<String> lengthTypes = new ArrayList<>();
    private List<String> weightTypes = new ArrayList<>();
    private enum ConversionType {
        TEMPERATURE,
        LENGTH,
        WEIGHT
    }
    
    public ConverterImpl() {
        super();
        // temperature types
        temperatureTypes.add("celsius");
        temperatureTypes.add("fahrenheit");
        
        // length types
        lengthTypes.add("kilometers");
        lengthTypes.add("miles");
        lengthTypes.add("feet");
        lengthTypes.add("yards");
        
        // weight types
        weightTypes.add("kilograms");
        weightTypes.add("pounds");
    }

    @Override
    public void convert(ConversionRequest req, StreamObserver<ConversionResponse> responseObserver) {
        System.out.println("Received from client: Convert value '" + req.getValue() + "' from " 
                            + req.getFromUnit() + " to " + req.getToUnit());
        ConversionResponse.Builder response = ConversionResponse.newBuilder();
        if (ConversionMatch(req.getFromUnit(), req.getToUnit()) != null) {
            // valid conversion
            response.setIsSuccess(true);
            response.setResult(convert(req.getValue(), req.getFromUnit(), req.getToUnit()));

        } else {
            // invalid conversion
            response.setIsSuccess(false);
            response.setError("Invalid conversion types");
        }

        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
    }

    // helper method for verifying conversion type compatibility
    public ConversionType ConversionMatch(String input1, String input2) {
        String normalizedInput1 = input1.toLowerCase();
        String normalizedInput2 = input2.toLowerCase();
        
        if (temperatureTypes.contains(normalizedInput1) && temperatureTypes.contains(normalizedInput2)) {
            return ConversionType.TEMPERATURE;
        }
        if (lengthTypes.contains(normalizedInput1) && lengthTypes.contains(normalizedInput2)) {
            return ConversionType.LENGTH;
        }
        if (weightTypes.contains(normalizedInput1) && weightTypes.contains(normalizedInput2)) {
            return ConversionType.WEIGHT;
        }
        return null;
    }

    // convertion logic helper
    public double convert(double value, String fromUnit, String toUnit) {
        double result = 0;
        String normalizedFromUnit = fromUnit.toLowerCase();
        String normalizedToUnit = toUnit.toLowerCase();
        
        switch (normalizedFromUnit) {
            case "kilometers":
                switch (toUnit) {
                    case "miles":
                        result = value * 0.621371;
                        break;
                    case "feet":
                        result = value * 3280.84;
                        break;
                    case "yards":
                        result = value * 1093.61;
                        break;
                    case "kilometers":
                        result = value;
                        break;
                }
                break;
            case "miles":
                switch (toUnit) {
                    case "kilometers":
                        result = value * 1.60934;
                        break;
                    case "feet":
                        result = value * 5280;
                        break;
                    case "yards":
                        result = value * 1760;
                        break;
                    case "miles":
                        result = value;
                        break;
                }
                break;
            case "feet":
                switch (toUnit) {
                    case "kilometers":
                        result = value / 3280.84;
                        break;
                    case "miles":
                        result = value / 5280;
                        break;
                    case "yards":
                        result = value / 3;
                        break;
                    case "feet":
                        result = value;
                        break;
                }
                break;
            case "yards":
                switch (toUnit) {
                    case "kilometers":
                        result = value / 1093.61;
                        break;
                    case "miles":
                        result = value / 1760;
                        break;
                    case "yards":
                        result = value;
                        break;
                    case "feet":
                        result = value * 3;
                        break;
                }
                break;
            case "kilograms":
                switch (toUnit) {
                    case "kilograms":
                        result = value;
                        break;
                    case "pounds":
                        result = value * 2.20462;
                        break;
                }
                break;
            case "pounds":
                switch (toUnit) {
                    case "kilograms":
                        result = value / 2.20462;
                        break;
                    case "pounds":
                        result = value;
                        break;
                }
                break;
            case "celsius":
                switch (toUnit) {
                    case "celsius":
                        result = value;
                        break;
                    case "fahrenheit":
                        result = (value * 9/5) + 32;
                        break;
                }
                break;
            case "fahrenheit":
                switch (toUnit) {
                    case "celsius":
                        result = (value - 32) * 5/9;
                        break;
                    case "fahrenheit":
                        result = value;
                        break;
                }
                break;
        }
        return result;
    }
}
