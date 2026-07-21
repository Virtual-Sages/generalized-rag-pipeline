import './Spinner.scss'

const Spinner = ({ size = "md" }) => {
    return <span className={`spinner spinner--${size}`}></span>;
};

export default Spinner;