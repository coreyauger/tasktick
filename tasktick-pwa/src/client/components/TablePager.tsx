import * as React from 'react';
import { Theme } from '@material-ui/core/styles/createMuiTheme';
import FirstPageIcon from '@material-ui/icons/FirstPage';
import KeyboardArrowLeft from '@material-ui/icons/KeyboardArrowLeft';
import KeyboardArrowRight from '@material-ui/icons/KeyboardArrowRight';
import LastPageIcon from '@material-ui/icons/LastPage';
import IconButton from '@material-ui/core/IconButton';
import createStyles from '@material-ui/core/styles/createStyles';
import withStyles, { WithStyles } from '@material-ui/core/styles/withStyles';

import withRoot from '../withRoot';

const styles = (theme: Theme) =>
  createStyles({
    root: {
      flexShrink: 0,
      color: theme.palette.text.secondary,
      marginLeft: theme.spacing.unit * 2.5,
    },
  });


interface Props {
  count: number;
  onChangePage: (Event, number) => void;
  page: number;
  rowsPerPage: number;  
  direction: "rtl" | "ltr";
};

class TablePager extends React.Component<Props & WithStyles<typeof styles>, undefined> {
 
  handleFirstPageButtonClick = event => {
    this.props.onChangePage(event, 0);
  };

  handleBackButtonClick = event => {
    this.props.onChangePage(event, this.props.page - 1);
  };

  handleNextButtonClick = event => {
    this.props.onChangePage(event, this.props.page + 1);
  };

  handleLastPageButtonClick = event => {
    this.props.onChangePage(
      event,
      Math.max(0, Math.ceil(this.props.count / this.props.rowsPerPage) - 1),
    );
  };

  render() {        
    const  classes = this.props.classes;
    return (
      <div className={classes.root}>
        <IconButton
          onClick={this.handleFirstPageButtonClick}
          disabled={this.props.page === 0}
          aria-label="First Page"
        >
          {this.props.direction === 'rtl' ? <LastPageIcon /> : <FirstPageIcon />}
        </IconButton>
        <IconButton
          onClick={this.handleBackButtonClick}
          disabled={this.props.page === 0}
          aria-label="Previous Page"
        >
          {this.props.direction === 'rtl' ? <KeyboardArrowRight /> : <KeyboardArrowLeft />}
        </IconButton>
        <IconButton
          onClick={this.handleNextButtonClick}
          disabled={this.props.page >= Math.ceil(this.props.count / this.props.rowsPerPage) - 1}
          aria-label="Next Page"
        >
          {this.props.direction === 'rtl' ? <KeyboardArrowLeft /> : <KeyboardArrowRight />}
        </IconButton>
        <IconButton
          onClick={this.handleLastPageButtonClick}
          disabled={this.props.page >= Math.ceil(this.props.count / this.props.rowsPerPage) - 1}
          aria-label="Last Page"
        >
          {this.props.direction === 'rtl' ? <FirstPageIcon /> : <LastPageIcon />}
        </IconButton>
      </div>
    );       
  }
}

export default withRoot(withStyles(styles)(TablePager));