const ctxWashingMachine = document.getElementById('washingMachineChart');
const ctxDryer = document.getElementById('dryerChart');

let knownTimeStampsWasher = [];
let knownTimeStampsDryer = [];
const washingMachineChart = new Chart(ctxWashingMachine, {
    type: 'line',
    data: {
        datasets: [{
            label: 'Wasmachine verbruik',
            borderColor: '#F00000',
            backgroundColor: '#F00000',
            data: []
        }]
    },
    options: {
        scales: {
            x: {
                type: 'time',
            },
            y: {
                title: {
                    text: 'Verbruik in milliwatt-uur',
                    display: true,
                },
                beginAtZero: true
            }
        }
    }
});

const dryerChart = new Chart(ctxDryer, {
    type: 'line',
    data: {
        datasets: [{
            label: 'Droger verbruik',
            borderColor: '#00F000',
            backgroundColor: '#00F000',
            data: []
        }]
    },
    options: {
        scales: {
            x: {
                type: 'time',
            },
            y: {
                title: {
                    text: 'Verbruik in milliwatt-uur',
                    display: true,
                },
                beginAtZero: true
            }
        }
    }
});

let updateChart = function () {
    $.getJSON('/api/v1/power/washingmachine',null, (data) => {
        $.each(data, (index) => {
            let item = data[index];
            if (knownTimeStampsWasher.indexOf(item.timeStamp) !== -1) {
                return;
            }

            knownTimeStampsWasher.push(item.timeStamp);
            washingMachineChart.data.datasets[0].data.push({
                x: item.timeStamp,
                y: item.measuredOutputInMilliWattHourMinute,
            })
        });
        washingMachineChart.update();
    });
    $.getJSON('/api/v1/power/dryer',null, (data) => {
        $.each(data, (index) => {
            let item = data[index];
            if (knownTimeStampsDryer.indexOf(item.timeStamp) !== -1) {
                return;
            }

            knownTimeStampsDryer.push(item.timeStamp);
            dryerChart.data.datasets[0].data.push({
                x: item.timeStamp,
                y: item.measuredOutputInMilliWattHourMinute,
            })
        });
        dryerChart.update();
    });
};

updateChart();
setInterval(updateChart, 60000);